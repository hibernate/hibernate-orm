/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementation for handling character data
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterCharacterData<T> extends BasicJdbcLiteralFormatter<T> {
	public static final String NATIONALIZED_PREFIX = "N";

	private final boolean isNationalized;

	public JdbcLiteralFormatterCharacterData(JavaType<T> javaType) {
		this( javaType, false );
	}

	public JdbcLiteralFormatterCharacterData(JavaType<T> javaType, boolean isNationalized) {
		super( javaType );
		this.isNationalized = isNationalized;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		final String literalValue = unwrap( value, String.class, wrapperOptions );
		if ( isNationalized ) {
			appender.appendSql( NATIONALIZED_PREFIX );
		}
		dialect.appendLiteral( appender, literalValue );
	}
}
