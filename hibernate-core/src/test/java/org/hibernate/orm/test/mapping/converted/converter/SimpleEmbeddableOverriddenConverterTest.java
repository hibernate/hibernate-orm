/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.sql.Types;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Tests MappedSuperclass/Entity overriding of Convert definitions
 *
 * @author Steve Ebersole
 */
public class SimpleEmbeddableOverriddenConverterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	/**
	 * Test outcome of annotations exclusively.
	 */
	@Test
	public void testSimpleConvertOverrides() {
		final EntityPersister ep = sessionFactory().getMappingMetamodel().getEntityDescriptor(Person.class.getName());
		final JdbcTypeRegistry jdbcTypeRegistry = sessionFactory().getTypeConfiguration()
				.getJdbcTypeRegistry();
		CompositeType homeAddressType = assertTyping( CompositeType.class, ep.getPropertyType( "homeAddress" ) );
		BasicType<?> homeAddressCityType = (BasicType<?>) findCompositeAttributeType( homeAddressType, "city" );
		assertTyping( StringJavaType.class, homeAddressCityType.getJavaTypeDescriptor() );
		assertTyping( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ).getClass(), homeAddressCityType.getJdbcType() );
	}

	public Type findCompositeAttributeType(CompositeType compositeType, String attributeName) {
		int pos = 0;
		for ( String name : compositeType.getPropertyNames() ) {
			if ( name.equals( attributeName ) ) {
				break;
			}
			pos++;
		}

		if ( pos >= compositeType.getPropertyNames().length ) {
			throw new IllegalStateException( "Could not locate attribute index for [" + attributeName + "] in composite" );
		}

		return compositeType.getSubtypes()[pos];
	}

	@Embeddable
	public static class Address {
		public String street;
		@Convert(converter = SillyStringConverter.class)
		public String city;
	}

	@Entity( name="Person" )
	public static class Person {
		@Id
		public Integer id;
		@Embedded
		@Convert( attributeName = "city", disableConversion = true )
		public Address homeAddress;
	}
}
