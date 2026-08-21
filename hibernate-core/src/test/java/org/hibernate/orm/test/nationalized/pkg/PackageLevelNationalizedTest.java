/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nationalized.pkg;

import java.sql.Types;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that {@code @Nationalized} applied to a package acts as a
 * default for every character-typed attribute of every entity in the
 * package.
 */
@BaseUnitTest
public class PackageLevelNationalizedTest {

	@Test
	public void test() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( NationalizedPackageEntity.class )
					.addPackage( NationalizedPackageEntity.class.getPackage().getName() )
					.buildMetadata();

			final Dialect dialect = metadata.getDatabase().getDialect();
			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();

			final PersistentClass pc =
					metadata.getEntityBinding( NationalizedPackageEntity.class.getName() );
			assertNotNull( pc );

			assertNationalized( pc.getProperty( "name" ),
					jdbcTypeRegistry, dialect, Types.VARCHAR, Types.NVARCHAR );
			assertNationalized( pc.getProperty( "initial" ),
					jdbcTypeRegistry, dialect, Types.CHAR, Types.NCHAR );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private static void assertNationalized(
			Property property,
			JdbcTypeRegistry jdbcTypeRegistry,
			Dialect dialect,
			int implicitCode,
			int explicitCode) {
		final BasicType<?> type = (BasicType<?>) property.getType();
		final int expected = dialect.getNationalizationSupport() == NationalizationSupport.EXPLICIT
				? explicitCode
				: implicitCode;
		assertSame( jdbcTypeRegistry.getDescriptor( expected ), type.getJdbcType() );
	}
}
