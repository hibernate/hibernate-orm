/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Use a converter without a {@code @Converter} annotation when it is registered using the
 * standard JPA XML {@code <converter auto-apply="true">} declaration in orm.xml.
 */
public class JpaOrmXmlRegisterConverterTest {
	@Test
	public void jpaXmlAutoApplyTakesPrecedenceOverManagedClassDiscovery() {
		final var registry = ServiceRegistryUtil.serviceRegistry();
		try {
			final var sources = new MetadataSources( registry )
					.addAnnotatedClassName( Customer.class.getName() )
					// Mimic a container supplying the converter among its managed classes.
					// Discovery registers it without an explicit auto-apply setting.
					.addAnnotatedClassName( EmailAddressConverter.class.getName() )
					// Standard JPA XML also declares this converter, explicitly enabling auto-apply.
					.addResource( "org/hibernate/test/converter/xml-auto-apply-overrides-managed-converter.orm.xml" );

			// Before the fix, discovery hid the XML setting and metadata boot failed:
			// EmailAddress has no JDBC mapping without its converter.
			final var metadata = sources
					.buildMetadata();
			final var value = (BasicValue) metadata.getEntityBinding( Customer.class.getName() )
					.getProperty( "emailAddress" ).getValue();
			final var type = assertInstanceOf( ConvertedBasicTypeImpl.class, value.getType() );
			// Auto-application must map the EmailAddress property to a JDBC String.
			assertEquals( EmailAddress.class, type.getJavaTypeDescriptor().getJavaTypeClass() );
			assertEquals( String.class, type.getJdbcJavaType().getJavaTypeClass() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	public static class Customer {
		public long id;
		public EmailAddress emailAddress;
	}

	public static class EmailAddress {
		public String value;
	}

	// Missing @Converter annotation; the converter will be registered in XML.
	public static class EmailAddressConverter implements AttributeConverter<EmailAddress, String> {
		@Override
		public String convertToDatabaseColumn(EmailAddress attribute) {
			return attribute == null ? null : attribute.value;
		}

		@Override
		public EmailAddress convertToEntityAttribute(String dbData) {
			if ( dbData == null ) {
				return null;
			}
			final var data = new EmailAddress();
			data.value = dbData;
			return data;
		}
	}
}
