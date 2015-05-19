/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Gives binding (nullSafeSet) and extracting (nullSafeGet) code access to options.
 *
 * @todo Definitely could use a better name
 *
 * @author Steve Ebersole
 */
public interface WrapperOptions {
	/**
	 * Should streams be used for binding LOB values.
	 *
	 * @return {@code true}/{@code false}
	 */
	public boolean useStreamForLobBinding();

	/**
	 * Obtain access to the {@link LobCreator}
	 *
	 * @return The LOB creator
	 */
	public LobCreator getLobCreator();

	/**
	 * Allow remapping of descriptors for dealing with sql type.
	 *
	 * @param sqlTypeDescriptor The known descriptor
	 *
	 * @return The remapped descriptor.  May be the same as the known descriptor indicating no remapping.
	 */
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor);
}
