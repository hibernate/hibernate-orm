package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.Type;

/**
 * Substitutes a <code>Type</code> for itself.
 *
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public class IdentityTypeEnvironment implements TypeEnvironment {

	public static final TypeEnvironment INSTANCE = new IdentityTypeEnvironment();

	private IdentityTypeEnvironment() {
	}

	public Type bind(Type type) {
		return type;
	}
    
    public String toString() {
        return "{}";
    }
}
