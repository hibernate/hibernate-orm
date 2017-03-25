/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.nationalized;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.StringType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * Test the use of {@link org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA}
 * to indicate that nationalized character data should be used.
 *
 * @author Steve Ebersole
 */
public class UseNationalizedCharDataSettingTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-10528" )
	public void testSetting() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, true )
				.build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedBySettingEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final PersistentClass pc = metadata.getEntityBinding( NationalizedBySettingEntity.class.getName() );
			final Property nameAttribute = pc.getProperty( "name" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				// See issue HHH-10693
				assertSame( StringType.INSTANCE, nameAttribute.getType() );
			}else {
				assertSame( StringNVarcharType.INSTANCE, nameAttribute.getType() );
			}

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11205" )
	public void testSettingOnCharType() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, true )
				.build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedBySettingEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final PersistentClass pc = metadata.getEntityBinding( NationalizedBySettingEntity.class.getName() );
			final Property nameAttribute = pc.getProperty( "flag" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				assertSame( CharacterType.INSTANCE, nameAttribute.getType() );
			}else {
				assertSame( CharacterNCharType.INSTANCE, nameAttribute.getType() );
			}

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "NationalizedBySettingEntity")
	@Table(name = "nationalized_by_setting_entity")
	public static class NationalizedBySettingEntity {
		@Id
		@GeneratedValue
		private long id;

		String name;
		char flag;
	}
}
