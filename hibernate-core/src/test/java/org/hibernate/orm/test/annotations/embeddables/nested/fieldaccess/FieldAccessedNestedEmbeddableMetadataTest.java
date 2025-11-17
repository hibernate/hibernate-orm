/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.nested.fieldaccess;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertJdbcTypeCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
public class FieldAccessedNestedEmbeddableMetadataTest {

	@Test
	public void testEnumTypeInterpretation() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();
			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();

			PersistentClass classMetadata = metadata.getEntityBinding( Customer.class.getName() );
			Property investmentsProperty = classMetadata.getProperty( "investments" );
			Collection investmentsValue = (Collection) investmentsProperty.getValue();
			Component investmentMetadata = (Component) investmentsValue.getElement();
			Value descriptionValue = investmentMetadata.getProperty( "description" ).getValue();
			assertEquals( 1, descriptionValue.getColumnSpan() );
			Column selectable = (Column) descriptionValue.getSelectables().get( 0 );
			assertEquals( (Long) 500L, selectable.getLength() );
			Component amountMetadata = (Component) investmentMetadata.getProperty( "amount" ).getValue();
			SimpleValue currencyMetadata = (SimpleValue) amountMetadata.getProperty( "currency" ).getValue();
			int[] currencySqlTypes = currencyMetadata.getType().getSqlTypeCodes( metadata );
			assertEquals( 1, currencySqlTypes.length );
			assertJdbcTypeCode(
					new int[] { jdbcTypeRegistry.getDescriptor( SqlTypes.VARCHAR ).getJdbcTypeCode(), SqlTypes.ENUM },
					currencySqlTypes[0]
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
