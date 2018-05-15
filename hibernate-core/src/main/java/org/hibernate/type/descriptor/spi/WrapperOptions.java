/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.spi;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.annotations.Remove;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Gives binding (nullSafeSet) and extracting (nullSafeGet) code access to options.
 *
 * @todo Definitely could use a better name
 *
 * @author Steve Ebersole
 *
 * @deprecated Just replace with Session
 */
@Remove
@Deprecated
public interface WrapperOptions {
	/**
	 * Should streams be used for binding LOB values.
	 *
	 * @return {@code true}/{@code false}
	 */
	boolean useStreamForLobBinding();

	/**
	 * Obtain access to the {@link LobCreator}
	 *
	 * @return The LOB creator
	 */
	LobCreator getLobCreator();

	/**
	 * Allow remapping of descriptors for dealing with sql type.
	 *
	 * @param sqlTypeDescriptor The known descriptor
	 *
	 * @return The remapped descriptor.  May be the same as the known descriptor indicating no remapping.
	 */

	SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor);

	/**
	 * The JDBC {@link TimeZone} used when persisting Timestamp and DateTime properties into the database.
	 * This setting is used when storing timestamps using the {@link java.sql.PreparedStatement#setTimestamp(int, Timestamp, Calendar)} method.
	 *
	 * This way, the storage {@link TimeZone} can differ from the default JVM TimeZone given by {@link TimeZone#getDefault()}.
	 *
	 * @return JDBC {@link TimeZone}
	 */
	TimeZone getJdbcTimeZone();

	SharedSessionContractImplementor getSession();
}
