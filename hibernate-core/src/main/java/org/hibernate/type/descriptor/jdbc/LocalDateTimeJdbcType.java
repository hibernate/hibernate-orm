/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.LocalDateTime;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain LocalDateTime} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeJdbcType extends AbstractJavaTimeJdbcType<LocalDateTime> {
	public static LocalDateTimeJdbcType INSTANCE = new LocalDateTimeJdbcType();

	public LocalDateTimeJdbcType() {
		super( LocalDateTime.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.LOCAL_DATE_TIME;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.TIMESTAMP;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}
}
