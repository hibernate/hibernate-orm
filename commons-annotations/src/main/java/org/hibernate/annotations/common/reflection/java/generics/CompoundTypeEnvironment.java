/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.Type;

/**
 * A composition of two <code>TypeEnvironment</code> functions.
 *
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public class CompoundTypeEnvironment implements TypeEnvironment {

	private final TypeEnvironment f;

	private final TypeEnvironment g;
    
    private final int hashCode;

    public static TypeEnvironment create(TypeEnvironment f, TypeEnvironment g) {
        if ( g == IdentityTypeEnvironment.INSTANCE )
            return f;
        if ( f == IdentityTypeEnvironment.INSTANCE )
            return g;
        return new CompoundTypeEnvironment( f, g );
    }
    
    private CompoundTypeEnvironment(TypeEnvironment f, TypeEnvironment g) {
		this.f = f;
		this.g = g;
        hashCode = doHashCode();
    }

	public Type bind(Type type) {
		return f.bind( g.bind( type ) );
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof CompoundTypeEnvironment ) ) return false;

		final CompoundTypeEnvironment that = (CompoundTypeEnvironment) o;

        if ( differentHashCode( that ) ) return false;

        if ( !f.equals( that.f ) ) return false;
        return g.equals( that.g );

    }

    private boolean differentHashCode(CompoundTypeEnvironment that) {
        return hashCode != that.hashCode;
    }

    private int doHashCode() {
		int result;
		result = f.hashCode();
		result = 29 * result + g.hashCode();
		return result;
	}

    public int hashCode() {
        //cached because the inheritance can be big
        return hashCode;
    }
    
    @Override
    public String toString() {
        return f.toString() + "(" + g.toString() + ")";
    }
}
