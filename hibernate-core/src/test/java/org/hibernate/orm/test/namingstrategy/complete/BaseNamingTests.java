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
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public abstract class BaseNamingTests {

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
			if ( metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	protected abstract void applySources(MetadataSources metadataSources);

	protected abstract ImplicitNamingStrategy getImplicitNamingStrategyToUse();

	protected void validateCustomer(Metadata metadata) {
		final PersistentClass customerBinding = metadata.getEntityBinding( Customer.class.getName() );
		assertThat( customerBinding ).isNotNull();

		validateCustomerPrimaryTableName( customerBinding.getTable().getQuotedName() );

		assertThat( customerBinding.getIdentifier().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerPrimaryKeyColumn( (Column) customerBinding.getIdentifier().getSelectables().get( 0 ) );

		assertThat( customerBinding.getVersion() ).isNotNull();
		assertThat( customerBinding.getVersion().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerVersionColumn( (Column) customerBinding.getVersion().getSelectables().get( 0 ) );

		final Property nameBinding = customerBinding.getProperty( "name" );
		assertThat( nameBinding ).isNotNull();
		assertThat( nameBinding.getColumnSpan() ).isEqualTo( 1 );
		validateCustomerNameColumn( (Column) nameBinding.getSelectables().get( 0 ) );

		final Property hqAddressBinding = customerBinding.getProperty( "hqAddress" );
		assertThat( hqAddressBinding ).isNotNull();
		assertThat( hqAddressBinding.getColumnSpan() ).isEqualTo( 3 );
		validateCustomerHqAddressComponent( assertTyping( Component.class, hqAddressBinding.getValue() ) );
	}

	protected abstract void validateCustomerPrimaryTableName(String name);

	protected abstract void validateCustomerPrimaryKeyColumn(Column column);

	protected abstract void validateCustomerVersionColumn(Column column);

	protected abstract void validateCustomerNameColumn(Column column);

	protected abstract void validateCustomerHqAddressComponent(Component component);


	protected void validateOrder(Metadata metadata) {
		final PersistentClass orderBinding = metadata.getEntityBinding( Order.class.getName() );
		assertThat( orderBinding ).isNotNull();

		validateOrderPrimaryTableName( orderBinding.getTable().getQuotedName() );

		assertThat( orderBinding.getIdentifier().getColumnSpan() ).isEqualTo( 1 );
		validateOrderPrimaryKeyColumn( (Column) orderBinding.getIdentifier().getSelectables().get( 0 ) );

		final Property referenceCodeBinding = orderBinding.getProperty( "referenceCode" );
		assertThat( referenceCodeBinding ).isNotNull();
		assertThat( referenceCodeBinding.getColumnSpan() ).isEqualTo( 1 );
		validateOrderReferenceCodeColumn( (Column) referenceCodeBinding.getSelectables().get( 0 ) );

		final Property placedBinding = orderBinding.getProperty( "placed" );
		assertThat( placedBinding ).isNotNull();
		assertThat( placedBinding.getColumnSpan() ).isEqualTo( 1 );
		validateOrderPlacedColumn( (Column) placedBinding.getSelectables().get( 0 ) );

		final Property fulfilledBinding = orderBinding.getProperty( "fulfilled" );
		assertThat( fulfilledBinding ).isNotNull();
		assertThat( fulfilledBinding.getColumnSpan() ).isEqualTo( 1 );
		validateOrderFulfilledColumn( (Column) fulfilledBinding.getSelectables().get( 0 ) );

		final Property customerBinding = orderBinding.getProperty( "customer" );
		assertThat( customerBinding ).isNotNull();
		assertThat( customerBinding.getColumnSpan() ).isEqualTo( 1 );
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
		assertThat( zipCodeBinding ).isNotNull();

		validateZipCodePrimaryTableName( zipCodeBinding.getTable().getQuotedName() );

		assertThat( zipCodeBinding.getIdentifier().getColumnSpan() ).isEqualTo( 1 );
		validateZipCodePrimaryKeyColumn( (Column) zipCodeBinding.getIdentifier().getSelectables().get( 0 ) );

		final Property codeBinding = zipCodeBinding.getProperty( "code" );
		assertThat( codeBinding ).isNotNull();
		assertThat( codeBinding.getColumnSpan() ).isEqualTo( 1 );
		validateZipCodeCodeColumn( (Column) codeBinding.getSelectables().get( 0 ) );

		final Property cityBinding = zipCodeBinding.getProperty( "city" );
		assertThat( cityBinding ).isNotNull();
		assertThat( cityBinding.getColumnSpan() ).isEqualTo( 1 );
		validateZipCodeCityColumn( (Column) cityBinding.getSelectables().get( 0 ) );

		final Property stateBinding = zipCodeBinding.getProperty( "state" );
		assertThat( stateBinding ).isNotNull();
		assertThat( stateBinding.getColumnSpan() ).isEqualTo( 1 );
		validateZipCodeStateColumn( (Column) stateBinding.getSelectables().get( 0 ) );
	}

	protected abstract void validateZipCodePrimaryTableName(String name);

	protected abstract void validateZipCodePrimaryKeyColumn(Column column);

	protected abstract void validateZipCodeCodeColumn(Column column);

	protected abstract void validateZipCodeCityColumn(Column column);

	protected abstract void validateZipCodeStateColumn(Column column);


	protected void validateCustomerRegisteredTrademarks(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding(
				Customer.class.getName() + ".registeredTrademarks" );
		assertThat( collectionBinding ).isNotNull();

		validateCustomerRegisteredTrademarksTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertThat( collectionBinding.getKey().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerRegisteredTrademarksKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertThat( collectionBinding.getElement().getColumnSpan() ).isEqualTo( 1 );
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
		assertThat( collectionBinding ).isNotNull();

		validateCustomerAddressesTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertThat( collectionBinding.getKey().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerAddressesKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertThat( collectionBinding.getElement().getColumnSpan() ).isEqualTo( 3 );
		validateCustomerAddressesElementComponent( assertTyping( Component.class, collectionBinding.getElement() ) );
	}

	protected abstract void validateCustomerAddressesTableName(String name);

	protected abstract void validateCustomerAddressesKeyColumn(Column column);

	protected abstract void validateCustomerAddressesElementComponent(Component component);


	protected void validateCustomerOrders(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".orders" );
		assertThat( collectionBinding ).isNotNull();

		validateCustomerOrdersTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertThat( collectionBinding.getKey().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerOrdersKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertThat( collectionBinding.getElement().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerOrdersElementColumn( (Column) collectionBinding.getElement().getSelectables().get( 0 ) );
	}

	protected abstract void validateCustomerOrdersTableName(String name);

	protected abstract void validateCustomerOrdersKeyColumn(Column column);

	protected abstract void validateCustomerOrdersElementColumn(Column column);

	protected void validateCustomerIndustries(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".industries" );
		assertThat( collectionBinding ).isNotNull();

		validateCustomerIndustriesTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertThat( collectionBinding.getKey().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerIndustriesKeyColumn( (Column) collectionBinding.getKey().getSelectables().get( 0 ) );

		assertThat( collectionBinding.getElement().getColumnSpan() ).isEqualTo( 1 );
		validateCustomerIndustriesElementColumn( (Column) collectionBinding.getElement().getSelectables().get( 0 ) );
	}

	public static <T> T assertTyping(Class<T> expectedType, Object value) {
		if ( !expectedType.isInstance( value ) ) {
			fail(
					String.format(
							"Expecting value of type [%s], but found [%s]",
							expectedType.getName(),
							value == null ? "<null>" : value
					)
			);
		}
		return (T) value;
	}

	protected abstract void validateCustomerIndustriesTableName(String name);

	protected abstract void validateCustomerIndustriesKeyColumn(Column column);

	protected abstract void validateCustomerIndustriesElementColumn(Column column);
}
