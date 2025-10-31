/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nationalized;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test the use of {@value AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA}
 * to indicate that nationalized character data should be used.
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
public class UseNationalizedCharDataSettingTest {

	@Test
	@JiraKey(value = "HHH-10528")
	public void testSetting() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, true )
				.build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedBySettingEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();
			final PersistentClass pc = metadata.getEntityBinding( NationalizedBySettingEntity.class.getName() );
			final Property nameAttribute = pc.getProperty( "name" );
			final BasicType<?> type = (BasicType<?>) nameAttribute.getType();
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertThat( type.getJavaTypeDescriptor() ).isSameAs( StringJavaType.INSTANCE );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				Assertions.assertSame( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ), type.getJdbcType() );
			}
			else {
				Assertions.assertSame( jdbcTypeRegistry.getDescriptor( Types.NVARCHAR ), type.getJdbcType() );
			}

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-11205")
	public void testSettingOnCharType() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, true )
				.build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedBySettingEntity.class );

			final Metadata metadata = ms.buildMetadata();
			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();
			final PersistentClass pc = metadata.getEntityBinding( NationalizedBySettingEntity.class.getName() );
			final Property nameAttribute = pc.getProperty( "flag" );
			final BasicType<?> type = (BasicType<?>) nameAttribute.getType();
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertThat( type.getJavaTypeDescriptor() ).isSameAs( CharacterJavaType.INSTANCE );
			if ( dialect.getNationalizationSupport() != NationalizationSupport.EXPLICIT ) {
				Assertions.assertSame( jdbcTypeRegistry.getDescriptor( Types.CHAR ), type.getJdbcType() );
			}
			else {
				Assertions.assertSame( jdbcTypeRegistry.getDescriptor( Types.NCHAR ), type.getJdbcType() );
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
