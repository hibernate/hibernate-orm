/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public abstract class BaseNamingTests extends BaseUnitTestCase {

	@Test
	public void doTest() {
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() );
		try {
			applySources( metadataSources );

			final Metadata metadata = metadataSources.getMetadataBuilder()
					.applyImplicitNamingStrategy( getImplicitNamingStrategyToUse() )
					.build();

			validateCustomer( metadata );
			validateOrder( metadata );
			validateZipCode( metadata );

			validateCustomerRegisteredTrademarks( metadata );
			validateCustomerAddresses( metadata );
			validateCustomerOrders( metadata );
			validateCustomerIndustries( metadata );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	protected abstract void applySources(MetadataSources metadataSources);

	protected abstract ImplicitNamingStrategy getImplicitNamingStrategyToUse();

	protected void validateCustomer(Metadata metadata) {
		final PersistentClass customerBinding = metadata.getEntityBinding( Customer.class.getName() );
		assertNotNull( customerBinding );

		validateCustomerPrimaryTableName( customerBinding.getTable().getQuotedName() );

		assertEquals( 1, customerBinding.getIdentifier().getColumnSpan() );
		validateCustomerPrimaryKeyColumn( (Column) customerBinding.getIdentifier().getSelectables().get( 0 ) );

		assertNotNull( customerBinding.getVersion() );
		assertEquals( 1, customerBinding.getVersion().getColumnSpan() );
		validateCustomerVersionColumn( (Column) customerBinding.getVersion().getSelectables().get( 0 ) );

		final Property nameBinding = customerBinding.getProperty( "name" );
		assertNotNull( nameBinding );
		assertEquals( 1, nameBinding.getColumnSpan() );
		validateCustomerNameColumn( (Column) nameBinding.getSelectables().get( 0 ) );

		final Property hqAddressBinding = customerBinding.getProperty( "hqAddress" );
		assertNotNull( hqAddressBinding );
		assertEquals( 3, hqAddressBinding.getColumnSpan() );
		validateCustomerHqAddressComponent( assertTyping( Component.class, hqAddressBinding.getValue() ) );
	}

	protected abstract void validateCustomerPrimaryTableName(String name);

	protected abstract void validateCustomerPrimaryKeyColumn(Column column);

	protected abstract void validateCustomerVersionColumn(Column column);

	protected abstract void validateCustomerNameColumn(Column column);

	protected abstract void validateCustomerHqAddressComponent(Component component);


	protected void validateOrder(Metadata metadata) {
		final PersistentClass orderBinding = metadata.getEntityBinding( Order.class.getName() );
		assertNotNull( orderBinding );

		validateOrderPrimaryTableName( orderBinding.getTable().getQuotedName() );

		assertEquals( 1, orderBinding.getIdentifier().getColumnSpan() );
		validateOrderPrimaryKeyColumn( (Column) orderBinding.getIdentifier().getSelectables().get( 0 ) );

		final Property referenceCodeBinding = orderBinding.getProperty( "referenceCode" );
		assertNotNull( referenceCodeBinding );
		assertEquals( 1, referenceCodeBinding.getColumnSpan() );
		validateOrderReferenceCodeColumn( (Column) referenceCodeBinding.getSelectables().get( 0 ) );

		final Property placedBinding = orderBinding.getProperty( "placed" );
		assertNotNull( placedBinding );
		assertEquals( 1, placedBinding.getColumnSpan() );
		validateOrderPlacedColumn( (Column) placedBinding.getSelectables().get( 0 ) );

		final Property fulfilledBinding = orderBinding.getProperty( "fulfilled" );
		assertNotNull( fulfilledBinding );
		assertEquals( 1, fulfilledBinding.getColumnSpan() );
		validateOrderFulfilledColumn( (Column) fulfilledBinding.getSelectables().get( 0 ) );

		final Property customerBinding = orderBinding.getProperty( "customer" );
		assertNotNull( customerBinding );
		assertEquals( 1, customerBinding.getColumnSpan() );
		validateOrderCustomerColumn( (Column) customerBinding.getSelectables().get( 0 ) );
	}

	protected abstract void validateOrderPrimaryTableName(String name);

	protected abstract void validateOrderPrimaryKeyColumn(Column column);

	protected abstract void validateOrderReferenceCodeColumn(Column column);

	protected abstract void validateOrderFulfilledColumn(Column column);

	protected abstract void validateOrderPlacedColumn(Column column);

	protected abstract void validateOrderCustomerColumn(Column column);



	protected void validateZipCode(Metadata metadata) {
		final PersistentClass zipCodeBinding = metadata.getEntityBinding( ZipCode.class.getName() );
		assertNotNull( zipCodeBinding );

		validateZipCodePrimaryTableName( zipCodeBinding.getTable().getQuotedName() );

		assertEquals( 1, zipCodeBinding.getIdentifier().getColumnSpan() );
		validateZipCodePrimaryKeyColumn( (Column) zipCodeBinding.getIdentifier().getSelectables().get( 0 ) );

		final Property codeBinding = zipCodeBinding.getProperty( "code" );
		assertNotNull( codeBinding );
		assertEquals( 1, codeBinding.getColumnSpan() );
		validateZipCodeCodeColumn( (Column) codeBinding.getSelectables().get( 0 ) );

		final Property cityBinding = zipCodeBinding.getProperty( "city" );
		assertNotNull( cityBinding );
		assertEquals( 1, cityBinding.getColumnSpan() );
		validateZipCodeCityColumn( (Column) cityBinding.getSelectables().get( 0 ) );

		final Property stateBinding = zipCodeBinding.getProperty( "state" );
		assertNotNull( stateBinding );
		assertEquals( 1, stateBinding.getColumnSpan() );
		validateZipCodeStateColumn( (Column) stateBinding.getSelectables().get( 0 ) );
	}

	protected abstract void validateZipCodePrimaryTableName(String name);

	protected abstract void validateZipCodePrimaryKeyColumn(Column column);

	protected abstract void validateZipCodeCodeColumn(Column column);

	protected abstract void validateZipCodeCityColumn(Column column);

	protected abstract void validateZipCodeStateColumn(Column column);


	protected void validateCustomerRegisteredTrademarks(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".registeredTrademarks" );
		assertNotNull( collectionBinding );

		validateCustomerRegisteredTrademarksTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertEquals( 1, collectionBinding.getKey().getColumnSpan() );
		validateCustomerRegisteredTrademarksKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerRegisteredTrademarksElementColumn(
				(Column) collectionBinding.getElement()
						.getSelectables()
						.get( 0 )
		);
	}

	protected abstract void validateCustomerRegisteredTrademarksTableName(String name);

	protected abstract void validateCustomerRegisteredTrademarksKeyColumn(Column column);

	protected abstract void validateCustomerRegisteredTrademarksElementColumn(Column column);


	protected void validateCustomerAddresses(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".addresses" );
		assertNotNull( collectionBinding );

		validateCustomerAddressesTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertEquals( 1, collectionBinding.getKey().getColumnSpan() );
		validateCustomerAddressesKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertEquals( 3, collectionBinding.getElement().getColumnSpan() );
		validateCustomerAddressesElementComponent( assertTyping( Component.class, collectionBinding.getElement() ) );
	}

	protected abstract void validateCustomerAddressesTableName(String name);

	protected abstract void validateCustomerAddressesKeyColumn(Column column);

	protected abstract void validateCustomerAddressesElementComponent(Component component);


	protected void validateCustomerOrders(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".orders" );
		assertNotNull( collectionBinding );

		validateCustomerOrdersTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertEquals( 1, collectionBinding.getKey().getColumnSpan() );
		validateCustomerOrdersKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerOrdersElementColumn( (Column) collectionBinding.getElement().getSelectables().get( 0 ) );
	}

	protected abstract void validateCustomerOrdersTableName(String name);

	protected abstract void validateCustomerOrdersKeyColumn(Column column);

	protected abstract void validateCustomerOrdersElementColumn(Column column);

	protected void validateCustomerIndustries(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".industries" );
		assertNotNull( collectionBinding );

		validateCustomerIndustriesTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertEquals( 1, collectionBinding.getKey().getColumnSpan() );
		validateCustomerIndustriesKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerIndustriesElementColumn( (Column) collectionBinding.getElement().getSelectables().get( 0 ) );
	}

	protected abstract void validateCustomerIndustriesTableName(String name);

	protected abstract void validateCustomerIndustriesKeyColumn(Column column);

	protected abstract void validateCustomerIndustriesElementColumn(Column column);
}
