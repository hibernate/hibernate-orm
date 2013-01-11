/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.procedure;

import javax.persistence.TemporalType;

/**
 * Describes an input value binding for any IN/INOUT parameters.
 */
public interface ParameterBind<T> {
	/**
	 * Retrieves the bound value.
	 *
	 * @return The bound value.
	 */
	public T getValue();

	/**
	 * If {@code <T>} represents a DATE/TIME type value, JPA usually allows specifying the particular parts of
	 * the DATE/TIME value to be bound.  This value represents the particular part the user requested to be bound.
	 *
	 * @return The explicitly supplied TemporalType.
	 */
	public TemporalType getExplicitTemporalType();
}
