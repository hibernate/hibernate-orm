/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.type.Type;

/**
 * An ordered pair of a value and its Hibernate type.
 * 
 * @see org.hibernate.type.Type
 * @author Gavin King
 */
public final class TypedValue implements Serializable {
	private final Type type;
	private final Object value;
	private final EntityMode entityMode;

	public TypedValue(Type type, Object value) {
		this( type, value, EntityMode.POJO );
	}

	public TypedValue(Type type, Object value, EntityMode entityMode) {
		this.type = type;
		this.value=value;
		this.entityMode = entityMode;
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return type;
	}

	public String toString() {
		return value==null ? "null" : value.toString();
	}

	public int hashCode() {
		//int result = 17;
		//result = 37 * result + type.hashCode();
		//result = 37 * result + ( value==null ? 0 : value.hashCode() );
		//return result;
		return value==null ? 0 : type.getHashCode(value );
	}

	public boolean equals(Object other) {
		if ( !(other instanceof TypedValue) ) return false;
		TypedValue that = (TypedValue) other;
		/*return that.type.equals(type) && 
			EqualsHelper.equals(that.value, value);*/
		return type.getReturnedClass() == that.type.getReturnedClass() &&
			type.isEqual(that.value, value );
	}

}





