/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.assertj.core.api.Assertions;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class EnumeratedSmokeTest {
	/**
	 * I personally have been unable to repeoduce the bug as reported in HHH-10402.  This test
	 * is equivalent to what the reporters say happens, but these tests pass fine.
	 */
	@Test
	@JiraKey( "HHH-10402" )
	public void testEnumeratedTypeResolutions(ServiceRegistryScope serviceRegistryScope) {
		final MetadataImplementor mappings = (MetadataImplementor) new MetadataSources( serviceRegistryScope.getRegistry() )
				.addAnnotatedClass( EntityWithEnumeratedAttributes.class )
				.buildMetadata();
		mappings.orderColumns( false );
		mappings.validate();

		final JdbcTypeRegistry jdbcTypeRegistry = mappings.getTypeConfiguration().getJdbcTypeRegistry();
		final PersistentClass entityBinding = mappings.getEntityBinding( EntityWithEnumeratedAttributes.class.getName() );

		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "notAnnotated" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "noEnumType" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "ordinalEnumType" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "stringEnumType" ), EnumType.STRING );
	}

	private void validateEnumMapping(JdbcTypeRegistry jdbcRegistry, Property property, EnumType expectedJpaEnumType) {
		final BasicType<?> propertyType = (BasicType<?>) property.getType();
//		final EnumValueConverter<?, ?> valueConverter = (EnumValueConverter<?, ?>) propertyType.getValueConverter();
		final JdbcMapping jdbcMapping = propertyType.getJdbcMapping();
		final JdbcType jdbcType = jdbcMapping.getJdbcType();

		assert expectedJpaEnumType != null;
		if ( expectedJpaEnumType == EnumType.ORDINAL ) {
//			Assertions.assertThat( valueConverter ).isInstanceOf( OrdinalEnumValueConverter.class );
			Assertions.assertThat( jdbcType.isInteger() ).isTrue();
		}
		else {
//			Assertions.assertThat( valueConverter ).isInstanceOf( NamedEnumValueConverter.class );
			Assertions.assertThat( jdbcType.isString() || jdbcType.getDefaultSqlTypeCode() == SqlTypes.ENUM ).isTrue();
		}
	}

	@Entity
	public static class EntityWithEnumeratedAttributes {
		@Id
		public Integer id;
		public Gender notAnnotated;
		@Enumerated
		public Gender noEnumType;
		@Enumerated(EnumType.ORDINAL)
		public Gender ordinalEnumType;
		@Enumerated(EnumType.STRING)
		public Gender stringEnumType;
	}

	public static enum Gender {
		MALE, FEMALE, UNKNOWN;
	}
}
