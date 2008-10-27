package org.jboss.envers.test.entities.customtype;

import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Component implements Serializable {
    private String prop1;
    private int prop2;

    public Component(String prop1, int prop2) {
        this.prop1 = prop1;
        this.prop2 = prop2;
    }

    public Component() {
    }

    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }

    public int getProp2() {
        return prop2;
    }

    public void setProp2(int prop2) {
        this.prop2 = prop2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Component)) return false;

        Component that = (Component) o;

        if (prop2 != that.prop2) return false;
        if (prop1 != null ? !prop1.equals(that.prop1) : that.prop1 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (prop1 != null ? prop1.hashCode() : 0);
        result = 31 * result + prop2;
        return result;
    }
}
