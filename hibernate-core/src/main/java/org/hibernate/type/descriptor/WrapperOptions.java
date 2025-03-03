/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

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
	default SessionFactoryImplementor getSessionFactory() {
		return getSession().getSessionFactory();
	}

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
	default boolean useStreamForLobBinding() {
		return getDialect().useInputStreamToInsertBlob();
	}

	/**
	 * The JDBC {@link java.sql.Types type code} used to bind a null boolean value.
	 *
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForBoolean()
	 */
	default int getPreferredSqlTypeCodeForBoolean() {
		return getSessionFactory().getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean();
	}

	/**
	 * Obtain access to the {@link LobCreator}.
	 *
	 * @return The LOB creator
	 *
	 * @see org.hibernate.cfg.JdbcSettings#NON_CONTEXTUAL_LOB_CREATION
	 * @see org.hibernate.dialect.Dialect#getDefaultNonContextualLobCreation()
	 */
	default LobCreator getLobCreator() {
		return getSession().getLobCreator();
	}

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
	 * @see org.hibernate.cfg.JdbcSettings#JDBC_TIME_ZONE
	 */
	default TimeZone getJdbcTimeZone() {
		return getSessionFactory().getSessionFactoryOptions().getJdbcTimeZone();
	}

	/**
	 * Obtain the {@link TypeConfiguration}.
	 *
	 * @since 7.0
	 */
	default TypeConfiguration getTypeConfiguration() {
		return getSessionFactory().getTypeConfiguration();
	}

	/**
	 * Obtain the XML {@link FormatMapper}.
	 *
	 * @since 7.0
	 */
	FormatMapper getXmlFormatMapper();

	/**
	 * Obtain the JSON {@link FormatMapper}.
	 *
	 * @since 7.0
	 */
	FormatMapper getJsonFormatMapper();
}
