package hero.bane.pvpbot.fakeplayer;

import com.mojang.authlib.GameProfile;
import hero.bane.pvpbot.PVPBotSettings;
import hero.bane.pvpbot.fakes.ServerPlayerInterface;
import hero.bane.pvpbot.mixin.ServerPlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerMPFake extends ServerPlayer {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final Set<String> spawning = new HashSet<>();

    public Runnable fixStartingPosition = () -> {
    };
    public boolean isAShadow;

    public Vec3 spawnPos;
    public double spawnYaw;

    // Returns true if it was successful, false if couldn't spawn due to the player not existing in Mojang servers
    public static boolean createFake(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimensionId, GameType gamemode, boolean flying) {
        //prolly half of that crap is not necessary, but it works
        ServerLevel worldIn = server.getLevel(dimensionId);
        server.services().nameToIdCache().resolveOfflineUsers(false);
        GameProfile gameprofile;

        UUID uuid = OldUsersConverter.convertMobOwnerIfNecessary(server, username);
        //NameAndId res = server.services().nameToIdCache().get(username).orElseThrow(); //findByName  .orElse(null)
        if (uuid == null && PVPBotSettings.allowSpawningOfflinePlayers) {
            server.services().nameToIdCache().resolveOfflineUsers(server.isDedicatedServer() && server.usesAuthentication());
            uuid = UUIDUtil.createOfflinePlayerUUID(username);
        }
        if (uuid == null) {
            return false; // no uuid, no player
        }
        gameprofile = new GameProfile(uuid, username);


        //GameProfile finalGP = gameprofile;

        // We need to mark this player as spawning so that we do not
        // try to spawn another player with the name while the profile
        // is being fetched - preventing multiple players spawning
        String name = gameprofile.name();
        spawning.add(name);

        fetchGameProfile(server, gameprofile.id()).whenCompleteAsync((p, t) -> {
            // Always remove the name, even if exception occurs
            spawning.remove(name);
            if (t != null) {
                return;
            }

            GameProfile current;
            if (p.name().isEmpty()) {
                current = gameprofile;
            } else {
                current = p;
            }

            EntityPlayerMPFake instance = new EntityPlayerMPFake(server, worldIn, current, ClientInformation.createDefault(), false);
            instance.fixStartingPosition = () -> instance.snapTo(pos.x, pos.y, pos.z, (float) yaw, (float) pitch);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, new CommonListenerCookie(current, 0, instance.clientInformation(), false));
            loadPlayerData(instance);
            instance.stopRiding(); // otherwise the created fake player will be on the vehicle
            instance.teleportTo(worldIn, pos.x, pos.y, pos.z, Set.of(), (float) yaw, (float) pitch, true);
            instance.setHealth(20.0F);
            instance.unsetRemoved();
            instance.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            instance.gameMode.changeGameModeForPlayer(gamemode);
            instance.spawnPos = pos;
            instance.spawnYaw = yaw;
            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);//instance.dimension);
            server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(instance), dimensionId);//instance.dimension);
            //instance.world.getChunkManager(). updatePosition(instance);
            instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show all model layers (incl. capes)
            instance.getAbilities().flying = flying;
        }, server);
        return true;
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID name) {
        final ResolvableProfile resolvableProfile = ResolvableProfile.createUnresolved(name);
        return resolvableProfile.resolveProfile(server.services().profileResolver());
    }

    private static void loadPlayerData(EntityPlayerMPFake player) {
        player.level().getServer().getPlayerList()
                .loadPlayerData(player.nameAndId())
                .map(tag -> TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        player.registryAccess(),
                        tag
                ))
                .ifPresent(valueInput -> {
                    player.load(valueInput);
                    player.loadAndSpawnEnderPearls(valueInput);
                    player.loadAndSpawnParentVehicle(valueInput);
                });
    }

    public static EntityPlayerMPFake createShadow(MinecraftServer server, ServerPlayer player) {
        player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        ServerLevel worldIn = player.level();//.getWorld(player.dimension);
        GameProfile gameprofile = player.getGameProfile();
        EntityPlayerMPFake playerShadow = new EntityPlayerMPFake(server, worldIn, gameprofile, player.clientInformation(), true);
        playerShadow.setChatSession(player.getChatSession());
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerShadow, new CommonListenerCookie(gameprofile, 0, player.clientInformation(), true));
        loadPlayerData(playerShadow);

        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        playerShadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
        ((ServerPlayerInterface) playerShadow).getActionPack().copyFrom(((ServerPlayerInterface) player).getActionPack());
        // this might create problems if a player logs back in...
        playerShadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
        playerShadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));


        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (player.yHeadRot * 256 / 360)), playerShadow.level().dimension());
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, playerShadow));
        //player.world.getChunkManager().updatePosition(playerShadow);
        playerShadow.getAbilities().flying = player.getAbilities().flying;
        return playerShadow;
    }

    public static EntityPlayerMPFake respawnFake(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        return new EntityPlayerMPFake(server, level, profile, cli, false);
    }

    public static boolean isSpawningPlayer(String username) {
        return spawning.contains(username);
    }

    private EntityPlayerMPFake(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli, boolean shadow) {
        super(server, worldIn, profile, cli);
        this.isAShadow = shadow;
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill(ServerLevel level) {
        kill(Component.literal("Killed"));
    }

    public void fakePlayerDisconnect(Component reason)
    {
        this.level().getServer().schedule(new TickTask(this.level().getServer().getTickCount(), () ->
                this.connection.onDisconnect(new DisconnectionDetails(reason))
        ));
    }

    public void kill(Component reason) {
        shakeOff();

        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            this.level().getServer().schedule(new TickTask(this.level().getServer().getTickCount(), () -> {
                this.connection.onDisconnect(new DisconnectionDetails(reason));
            }));
        }
    }

    @Override
    public void tick() {
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game not gonna crash violently.
        }
    }

    @Override
    public boolean startRiding(Entity entityToRide, boolean force, boolean sendEventAndTriggers) {
        if (super.startRiding(entityToRide, force, sendEventAndTriggers)) {
            // from ClientPacketListener.handleSetEntityPassengersPacket
            if (entityToRide instanceof AbstractBoat) {
                this.yRotO = entityToRide.getYRot();
                this.setYRot(entityToRide.getYRot());
                this.setYHeadRot(entityToRide.getYRot());
            }
            return true;
        } else {
            return false;
        }
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(DamageSource cause)
    {
        shakeOff();
        super.die(cause);

        MinecraftServer server = this.level().getServer();

        executor.schedule(() -> server.execute(() -> {
            ServerPlayer newPlayer = server.getPlayerList().respawn(
                    this,
                    false,
                    Entity.RemovalReason.KILLED
            );

            if (newPlayer instanceof EntityPlayerMPFake fake) {
                fake.setHealth(20.0F);
                fake.foodData = new FoodData();
                fake.giveExperienceLevels(-(fake.experienceLevel + 1));

                fake.teleportTo(fake.spawnPos.x, fake.spawnPos.y, fake.spawnPos.z);
                fake.setYRot((float) fake.spawnYaw);
                fake.setYHeadRot((float) fake.spawnYaw);
                fake.setDeltaMovement(0.0D, 0.0D, 0.0D);
            }
        }), 1L, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return PVPBotSettings.allowListingFakePlayers;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource damageSource)
    {
        return super.isInvulnerableTo(serverLevel, damageSource)
                || this.isChangingDimension() && !damageSource.is(DamageTypes.ENDER_PEARL);
    }

    @Override
    public ServerPlayer teleport(TeleportTransition serverLevel) {
        super.teleport(serverLevel);
        if (wonGame) {
            ServerboundClientCommandPacket p = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
            connection.handleClientCommand(p);
        }

        // If above branch was taken, *this* has been removed and replaced, the new instance has been set
        // on 'our' connection (which is now theirs, but we still have a ref).
        if (connection.player.isChangingDimension()) {
            connection.player.hasChangedDimension();
        }
        return connection.player;
    }

    @Override
    protected void blockUsingItem(ServerLevel serverLevel, LivingEntity livingEntity) {
        ItemStack itemStack = this.getItemBlockingWith();
        BlocksAttacks blocksAttacks = itemStack != null ? (BlocksAttacks) itemStack.get(DataComponents.BLOCKS_ATTACKS) : null;
        float f = livingEntity.getSecondsToDisableBlocking();
        if (f > 0.0F && blocksAttacks != null) {
            blocksAttacks.disable(serverLevel, this, f, itemStack);
            this.invulnerableTime = 20;
            if (PVPBotSettings.shieldStunning) {
                executor.schedule(() -> this.invulnerableTime = 0, 1, TimeUnit.MILLISECONDS);
            }
        }
    }
}