/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.naming.ImplicitDiscriminatorColumnNameSource;

/**
 * Contract for sources of information about a mapped discriminator.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public interface DiscriminatorSource extends ImplicitDiscriminatorColumnNameSource {
	/**
	 * Obtain the column/formula information about the discriminator.
	 *
	 * @return The column/formula information
	 */
	public RelationalValueSource getDiscriminatorRelationalValueSource();

	/**
	 * Obtain the discriminator type.  Unlike the type of attributes, implementors
	 * here should generally specify the type in case of no user selection rather than
	 * return null because we cannot infer it from any physical java member.
	 *
	 * @return The discriminator type
	 */
	public String getExplicitHibernateTypeName();

	/**
	 * "Forces" Hibernate to specify the allowed discriminator values, even when retrieving
	 * all instances of the root class.
	 *
	 * @return {@code true} in case the discriminator value should be forces, {@code false}
	 * otherwise. Default is {@code false}.
	 */
	boolean isForced();

	/**
	 * Set this to {@code false}, if your discriminator column is also part of a mapped
	 * composite identifier.  It tells Hibernate not to include the column in SQL INSERTs.
	 *
	 * @return {@code true} in case the discriminator value should be included in inserts,
	 * {@code false} otherwise.  Default is {@code true}.
	 */
	boolean isInserted();
}
