package org.hibernate.ejb.criteria.jpaMapMode;

public final class Column {
    private final String name;
    private final int type; // from java.sql.Type

    public Column(String name, int type) {
        super();
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o.getClass() != Column.class) {
            return false;
        }

        Column that = (Column) o;

        return this.name.equals(that.name) && this.type == that.type;
    }

    @Override
    public String toString() {
        return this.name + this.type;
    }

}
