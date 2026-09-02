/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Method;

import org.hibernate.dialect.type.spi.DB2JdbcTypes;
import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.type.spi.MariaDBJdbcTypes;
import org.hibernate.dialect.type.spi.MySQLJdbcTypes;
import org.hibernate.dialect.type.spi.OracleJdbcTypes;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.dialect.type.spi.SQLServerJdbcTypes;
import org.hibernate.dialect.type.spi.SpannerJdbcTypes;
import org.hibernate.dialect.type.spi.SybaseJdbcTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the supported access surface for Hibernate's stock
/// dialect-specific JDBC types.
///
/// @author Steve Ebersole
public class JdbcTypeAccessTests {
	private static final Class<?>[] FACADES = {
			DB2JdbcTypes.class,
			H2JdbcTypes.class,
			MariaDBJdbcTypes.class,
			MySQLJdbcTypes.class,
			OracleJdbcTypes.class,
			PostgreSQLJdbcTypes.class,
			SQLServerJdbcTypes.class,
			SpannerJdbcTypes.class,
			SybaseJdbcTypes.class
	};

	@Test
	void returnsStableInternalStockImplementations() throws ReflectiveOperationException {
		for ( Class<?> facade : FACADES ) {
			for ( Method method : facade.getDeclaredMethods() ) {
				if ( method.getParameterCount() == 0
						&& !method.isSynthetic()
						&& !method.getName().startsWith( "oson" ) ) {
					final Object first = method.invoke( null );
					final Object second = method.invoke( null );
					assertThat( first ).as( facade.getSimpleName() + "." + method.getName() ).isSameAs( second );
					assertThat( first ).isInstanceOfAny( JdbcType.class, JdbcTypeConstructor.class );
					assertThat( first.getClass().getPackageName() ).isEqualTo( "org.hibernate.dialect.type.internal" );
				}
			}
		}
	}
}
