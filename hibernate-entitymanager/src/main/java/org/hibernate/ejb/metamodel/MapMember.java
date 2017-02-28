package org.hibernate.ejb.metamodel;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * Defines a Member implementation used to represent attibutes that
 * aren't defined my fields or methods for non-class-based metamodels.
 *
 * @author Brad Koehn
 */
public class MapMember implements Member {
    private String name;
    private final Class<?> type;

    public MapMember( String name, Class<?> type ) {
        this.name = name;
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public int getModifiers() {
        return Modifier.PUBLIC;
    }

    public boolean isSynthetic() {
        return false;
    }

    public String getName() {
        return name;
    }

    public Class<?> getDeclaringClass() {
        return null;
    }
}
