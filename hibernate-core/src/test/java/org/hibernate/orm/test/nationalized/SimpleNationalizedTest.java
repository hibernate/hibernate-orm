/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nationalized;

import java.sql.NClob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.NClobJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongNVarcharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NCharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NClobJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
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

		@Type(type = "ntext")
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			BasicType<?> type = (BasicType<?>) prop.getType();
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( VarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( NVarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				if ( dialect instanceof SybaseDialect ) {
					assertSame( ClobJdbcTypeDescriptor.CLOB_BINDING, type.getJdbcTypeDescriptor() );
				}
				else {
					assertSame( ClobJdbcTypeDescriptor.DEFAULT, type.getJdbcTypeDescriptor() );
				}
			}
			else {
				assertSame( NClobJdbcTypeDescriptor.DEFAULT, type.getJdbcTypeDescriptor() );
			}
			prop = pc.getProperty( "nclobAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( NClobJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				if ( dialect instanceof SybaseDialect ) {
					assertSame( ClobJdbcTypeDescriptor.CLOB_BINDING, type.getJdbcTypeDescriptor() );
				}
				else {
					assertSame( ClobJdbcTypeDescriptor.DEFAULT, type.getJdbcTypeDescriptor() );
				}
			}
			else {
				assertSame( NClobJdbcTypeDescriptor.DEFAULT, type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( StringJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				assertSame( LongVarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( LongNVarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}

			prop = pc.getProperty( "ncharArrAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( CharacterArrayJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( VarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( NVarcharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
			prop = pc.getProperty( "ncharacterAtt" );
			type = (BasicType<?>) prop.getType();
			assertSame( CharacterJavaTypeDescriptor.INSTANCE, type.getJavaTypeDescriptor() );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				// See issue HHH-10693
				assertSame( CharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
			else {
				assertSame( NCharJdbcTypeDescriptor.INSTANCE, type.getJdbcTypeDescriptor() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
