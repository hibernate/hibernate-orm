/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.sql.Types;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple application of Convert annotation via XML.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SimpleXmlOverriddenTest {

	/**
	 * A baseline test, with an explicit @Convert annotation that should be in effect
	 */
	@Test
	public void baseline(ServiceRegistryScope scope) {
		Metadata metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( TheEntity.class )
				.buildMetadata();

		PersistentClass pc = metadata.getEntityBinding( TheEntity.class.getName() );
		Type type = pc.getProperty( "it" ).getType();
		ConvertedBasicTypeImpl adapter = assertTyping( ConvertedBasicTypeImpl.class, type );
		final JpaAttributeConverter converter = (JpaAttributeConverter) adapter.getValueConverter();
		assertTrue( SillyStringConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the attribute level
	 */
	@Test
	public void testDefinitionAtAttributeLevel(ServiceRegistryScope scope) {
		// NOTE : simple-override.xml applied disable-conversion="true" at the attribute-level
		Metadata metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( TheEntity.class )
				.addResource( "org/hibernate/test/converter/simple-override.xml" )
				.buildMetadata();
		final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase().getTypeConfiguration()
				.getJdbcTypeRegistry();

		PersistentClass pc = metadata.getEntityBinding( TheEntity.class.getName() );
		BasicType<?> type = (BasicType<?>) pc.getProperty( "it" ).getType();
		assertTyping( StringJavaType.class, type.getJavaTypeDescriptor() );
		assertTyping( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ).getClass(), type.getJdbcType() );
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the entity level
	 */
	@Test
	public void testDefinitionAtEntityLevel(ServiceRegistryScope scope) {
		// NOTE : simple-override2.xml applied disable-conversion="true" at the entity-level
		Metadata metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( TheEntity2.class )
				.addResource( "org/hibernate/test/converter/simple-override2.xml" )
				.buildMetadata();
		final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase().getTypeConfiguration()
				.getJdbcTypeRegistry();

		PersistentClass pc = metadata.getEntityBinding( TheEntity2.class.getName() );
		BasicType<?> type = (BasicType<?>) pc.getProperty( "it" ).getType();
		assertTyping( StringJavaType.class, type.getJavaTypeDescriptor() );
		assertTyping( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ).getClass(), type.getJdbcType() );
	}

	@Entity(name="TheEntity")
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert(converter = SillyStringConverter.class)
		public String it;
	}

	@Entity(name="TheEntity2")
	@Convert( attributeName = "it", converter = SillyStringConverter.class )
	public static class TheEntity2 {
		@Id
		public Integer id;
		public String it;
	}
}
