/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

/**
 * Contract for sources of information about a mapped discriminator.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public interface DiscriminatorSource {
	/**
	 * Obtain the column/formula information about the discriminator.
	 *
	 * @return The column/formula information
	 */
	public RelationalValueSource getDiscriminatorRelationalValueSource();

	/**
	 * Obtain the discriminator type.  Unlike the type of attributes, implementors here should generally specify the type
	 * in case of no user selection rather than return null because we cannot infer it from any physical java member.
	 *
	 * @return The discriminator type
	 */
	public String getExplicitHibernateTypeName();

	/**
	 * "Forces" Hibernate to specify the allowed discriminator values, even when retrieving all instances of the
	 * root class.
	 *
	 * @return {@code true} in case the discriminator value should be forces, {@code false} otherwise. Default
	 * is {@code false}.
	 */
	boolean isForced();

	/**
	 * Set this to {@code false}, if your discriminator column is also part of a mapped composite identifier.
	 * It tells Hibernate not to include the column in SQL INSERTs.
	 *
	 * @return {@code true} in case the discriminator value should be included in inserts, {@code false} otherwise.
	 * Default is {@code true}.
	 */
	boolean isInserted();
}
