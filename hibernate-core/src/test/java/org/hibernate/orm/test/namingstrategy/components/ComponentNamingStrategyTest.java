/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.components;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.List;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ComponentNamingStrategyTest {

	@Test
	public void testDefaultNamingStrategy() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.build();

		try {
			final Metadata metadata = MetadataBuildingTestHelper.buildMetadataWithImplicitNaming(
					ssr,
					new MappingSources()
							.addManagedClass( Container.class )
							.addManagedClass( Item.class ),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE
			);

			final PersistentClass pc = metadata.getEntityBinding( Container.class.getName() );
			Property p = pc.getProperty( "items" );
			List value = assertTyping( List.class, p.getValue() );
			SimpleValue elementValue = assertTyping( SimpleValue.class, value.getElement() );
			assertEquals( 1, elementValue.getColumnSpan() );
			Column column = assertTyping( Column.class, elementValue.getSelectables().get( 0 ) );
			assertEquals( column.getName(), "name" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey( value = "HHH-6005" )
	public void testComponentSafeNamingStrategy() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.build();

		try {
			final Metadata metadata = MetadataBuildingTestHelper.buildMetadataWithImplicitNaming(
					ssr,
					new MappingSources()
							.addManagedClass( Container.class )
							.addManagedClass( Item.class ),
					ImplicitNamingStrategyComponentPathImpl.INSTANCE
			);

			final PersistentClass pc = metadata.getEntityBinding( Container.class.getName() );
			Property p = pc.getProperty( "items" );
			List value = assertTyping( List.class, p.getValue() );
			SimpleValue elementValue = assertTyping(  SimpleValue.class, value.getElement() );
			assertEquals( 1, elementValue.getColumnSpan() );
			Column column = assertTyping( Column.class, elementValue.getSelectables().get( 0 ) );
			assertEquals( "items_name", column.getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
