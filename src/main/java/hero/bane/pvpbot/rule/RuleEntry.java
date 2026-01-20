package hero.bane.pvpbot.rule;

import java.lang.reflect.Field;

public final class RuleEntry {

    public final String name;
    public final String description;
    public final Class<?> type;
    private final Field field;

    public RuleEntry(Field field, Rule rule) {
        this.field = field;
        this.name = field.getName();
        this.description = rule.desc();
        this.type = field.getType();
        field.setAccessible(true);
    }

    public Object get() {
        try {
            return field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void set(Object value) {
        try {
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
