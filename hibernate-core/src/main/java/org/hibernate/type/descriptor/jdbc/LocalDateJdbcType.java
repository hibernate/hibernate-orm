/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.LocalDate;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain LocalDate} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class LocalDateJdbcType extends AbstractJavaTimeJdbcType<LocalDate> {
	public static LocalDateJdbcType INSTANCE = new LocalDateJdbcType();

	public LocalDateJdbcType() {
		super( LocalDate.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.LOCAL_DATE;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.DATE;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.DATE );
	}
}
