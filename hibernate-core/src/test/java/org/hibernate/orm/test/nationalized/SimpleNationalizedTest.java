/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nationalized;

import java.sql.NClob;
import java.sql.Types;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.NClobJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SimpleNationalizedTest {

	@SuppressWarnings("unused")
	@Entity(name = "NationalizedEntity")
	public static class NationalizedEntity {
		@Id
		private Integer id;

		@Nationalized
		private String nvarcharAtt;

		@Lob
		@Nationalized
		private String materializedNclobAtt;

		@Lob
		@Nationalized
		private NClob nclobAtt;

		@Nationalized
		private Character ncharacterAtt;

		@Nationalized
		@JavaType( CharacterArrayJavaType.class )
		private Character[] ncharArrAtt;

		@Lob
		@Nationalized
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			BasicType<?> type = (BasicType<?>) prop.getType();
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertSame( StringJavaType.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NVARCHAR ), type.getJdbcType() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaType.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcType() );
			}
			prop = pc.getProperty( "nclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( NClobJavaType.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcType() );
			}

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaType.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcType() );
			}

			prop = pc.getProperty( "ncharArrAtt" );
			type = (BasicType<?>) prop.getType();
			assertInstanceOf( CharacterArrayJavaType.class, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NVARCHAR ), type.getJdbcType() );
			}
			prop = pc.getProperty( "ncharacterAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( CharacterJavaType.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CHAR ), type.getJdbcType() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCHAR ), type.getJdbcType() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
