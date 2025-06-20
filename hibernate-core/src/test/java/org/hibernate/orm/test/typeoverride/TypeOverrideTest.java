/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.typeoverride;

import java.sql.Types;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * @author Gail Badner
 */
public class TypeOverrideTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/typeoverride/Entity.hbm.xml" };
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyBasicType( StoredPrefixedStringType.INSTANCE );
	}

	@Test
	public void testStandardBasicSqlTypeDescriptor() {
		final Dialect dialect = getMetadata().getDatabase().getDialect();
		final JdbcTypeRegistry jdbcTypeRegistry = getMetadata().getTypeConfiguration()
				.getJdbcTypeRegistry();
		// no override
		assertSame( IntegerJdbcType.INSTANCE, jdbcTypeRegistry.getDescriptor( Types.INTEGER ) );

		// A few dialects explicitly override BlobTypeDescriptor.DEFAULT
		if ( CockroachDialect.class.isInstance( dialect ) ) {
			assertSame(
					VarbinaryJdbcType.INSTANCE,
					jdbcTypeRegistry.getDescriptor( Types.BLOB )
			);
		}
		else if ( PostgreSQLDialect.class.isInstance( dialect ) ) {
			assertSame(
					BlobJdbcType.BLOB_BINDING,
					jdbcTypeRegistry.getDescriptor( Types.BLOB )
			);
		}
		else if ( SybaseDialect.class.isInstance( dialect ) ) {
			assertSame(
					BlobJdbcType.PRIMITIVE_ARRAY_BINDING,
					jdbcTypeRegistry.getDescriptor( Types.BLOB )
			);
		}
		else if ( HANADialect.class.isInstance( dialect ) ) {
			Assertions.assertInstanceOf(
					HANADialect.HANABlobType.class,
					jdbcTypeRegistry.getDescriptor( Types.BLOB )
			);
		}
		else {
			assertSame(
					BlobJdbcType.DEFAULT,
					jdbcTypeRegistry.getDescriptor( Types.BLOB )
			);
		}
	}

	@AfterEach
	public void tearDown() {
		sessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsert() {
		Entity e = new Entity( "name" );
		inTransaction(
				session ->
						session.persist( e )
		);

		inTransaction(
				session -> {
					Entity entity = session.get( Entity.class, e.getId() );
					assertFalse( entity.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
					assertEquals( "name", entity.getName() );
					session.remove( entity );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "HHH-6426")
	public void testRegisteredFunction() {
		Entity e = new Entity( "name " );
		inTransaction(
				session ->
						session.persist( e )
		);

		inTransaction(
				session -> {
					Entity entity = session.get( Entity.class, e.getId() );
					assertFalse( entity.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
					assertEquals( "name ", entity.getName() );
				}
		);

		inTransaction(
				session ->
						session.remove( e )
		);
	}
}
