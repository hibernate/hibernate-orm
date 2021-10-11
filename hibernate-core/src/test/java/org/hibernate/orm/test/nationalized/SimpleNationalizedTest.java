/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nationalized;

import java.sql.NClob;
import java.sql.Types;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

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
import org.hibernate.type.descriptor.java.CharacterArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.NClobJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SimpleNationalizedTest {

	@SuppressWarnings({ "UnusedDeclaration", "SpellCheckingInspection" })
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
		private Character[] ncharArrAtt;

		@Lob
		@Nationalized
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final JdbcTypeDescriptorRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeDescriptorRegistry();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			BasicType<?> type = (BasicType<?>) prop.getType();
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NVARCHAR ), type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcTypeDescriptor() );
			}
			prop = pc.getProperty( "nclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( NClobJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CLOB ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCLOB ), type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "ncharArrAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( CharacterArrayJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NVARCHAR ), type.getJdbcTypeDescriptor() );
			}
			prop = pc.getProperty( "ncharacterAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( CharacterJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.CHAR ), type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( jdbcTypeRegistry.getDescriptor( Types.NCHAR ), type.getJdbcTypeDescriptor() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
