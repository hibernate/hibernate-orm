/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source information related to mapping the multi-tenancy of an entity
 *
 * @author Steve Ebersole
 */
public interface MultiTenancySource {
	/**
	 * Obtain the column/formula information about the multi-tenancy discriminator.
	 *
	 * @return The column/formula information
	 */
	public RelationalValueSource getRelationalValueSource();

	public boolean isShared();

	public boolean bindAsParameter();
}
