/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;
import java.io.Serializable;

/**
 * Additional contract for primitive / primitive wrapper types.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PrimitiveType<T> extends LiteralType<T> {
	/**
	 * Retrieve the primitive counterpart to the wrapper type identified by
	 * {@link org.hibernate.type.Type#getReturnedClass()}.
	 *
	 * @return The primitive Java type.
	 */
	public abstract Class getPrimitiveClass();

	/**
	 * Retrieve the string representation of the given value.
	 *
	 * @param value The value to be stringified.
	 *
	 * @return The string representation
	 */
	public String toString(T value);

	/**
	 * Get this type's default value.
	 *
	 * @return The default value.
	 */
	public abstract Serializable getDefaultValue();
}
