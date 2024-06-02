/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.TimeZone;

/**
 * Options for {@linkplain ValueBinder#bind(java.sql.PreparedStatement, Object, int, WrapperOptions)
 * binding values to} and {@linkplain ValueExtractor#extract(java.sql.ResultSet, int, WrapperOptions)
 * extracting values from} JDBC prepared statements and result sets.
 *
 * @author Steve Ebersole
 *
 * @see ValueBinder
 * @see ValueExtractor
 */
public interface WrapperOptions {

	/**
	 * Access to the current session.
	 */
	SharedSessionContractImplementor getSession();

	/**
	 * Access to the current session factory.
	 */
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Access to the current dialect.
	 */
	default Dialect getDialect() {
		return getSessionFactory().getJdbcServices().getDialect();
	}

	/**
	 * Determines whether streams should be used for binding LOB values.
	 *
	 * @return {@code true}/{@code false}
	 *
	 * @see org.hibernate.dialect.Dialect#useInputStreamToInsertBlob()
	 */
	boolean useStreamForLobBinding();

	/**
	 * The JDBC {@link java.sql.Types type code} used to bind a null boolean value.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForBoolean()
	 */
	int getPreferredSqlTypeCodeForBoolean();

	/**
	 * Obtain access to the {@link LobCreator}.
	 *
	 * @return The LOB creator
	 *
	 * @see org.hibernate.cfg.AvailableSettings#NON_CONTEXTUAL_LOB_CREATION
	 * @see org.hibernate.dialect.Dialect#getDefaultNonContextualLobCreation()
	 */
	LobCreator getLobCreator();

	/**
	 * The JDBC {@link TimeZone} used when writing a value of type {@link java.sql.Time}
	 * or {@link java.sql.Timestamp} to a JDBC {@link java.sql.PreparedStatement}, or
	 * when reading from a JDBC {@link java.sql.ResultSet}.
	 * <ul>
	 * <li>When {@code getJdbcTimeZone()} is null, the method
	 *     {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)} is
	 *     called to write a timestamp, and
	 *     {@link java.sql.ResultSet#getTimestamp(int)} is called to read a timestamp.
	 * <li>But when not null, the method
	 *     {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)}
	 *     is called to write a timestamp, and
	 *     {@link java.sql.ResultSet#getTimestamp(int, java.util.Calendar)} is called to
	 *     read a timestamp.
	 * </ul>
	 * <p>
	 * Thus, the storage {@link TimeZone} can differ from the default JVM TimeZone given
	 * by {@link TimeZone#getDefault()}.
	 *
	 * @return the JDBC {@link TimeZone}, or null if no JDBC timezone was explicitly set
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JDBC_TIME_ZONE
	 */
	TimeZone getJdbcTimeZone();
}
