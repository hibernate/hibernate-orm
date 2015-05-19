/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.junit4.BaseUnitTestCase;
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
		final MetadataSources metadataSources = new MetadataSources();
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

	protected abstract void applySources(MetadataSources metadataSources);

	protected abstract ImplicitNamingStrategy getImplicitNamingStrategyToUse();

	protected void validateCustomer(Metadata metadata) {
		final PersistentClass customerBinding = metadata.getEntityBinding( Customer.class.getName() );
		assertNotNull( customerBinding );

		validateCustomerPrimaryTableName( customerBinding.getTable().getQuotedName() );

		assertEquals( 1, customerBinding.getIdentifier().getColumnSpan() );
		validateCustomerPrimaryKeyColumn( (Column) customerBinding.getIdentifier().getColumnIterator().next() );

		assertNotNull( customerBinding.getVersion() );
		assertEquals( 1, customerBinding.getVersion().getColumnSpan() );
		validateCustomerVersionColumn( (Column) customerBinding.getVersion().getColumnIterator().next() );

		final Property nameBinding = customerBinding.getProperty( "name" );
		assertNotNull( nameBinding );
		assertEquals( 1, nameBinding.getColumnSpan() );
		validateCustomerNameColumn( (Column) nameBinding.getColumnIterator().next() );

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
		validateOrderPrimaryKeyColumn( (Column) orderBinding.getIdentifier().getColumnIterator().next() );

		final Property referenceCodeBinding = orderBinding.getProperty( "referenceCode" );
		assertNotNull( referenceCodeBinding );
		assertEquals( 1, referenceCodeBinding.getColumnSpan() );
		validateOrderReferenceCodeColumn( (Column) referenceCodeBinding.getColumnIterator().next() );

		final Property placedBinding = orderBinding.getProperty( "placed" );
		assertNotNull( placedBinding );
		assertEquals( 1, placedBinding.getColumnSpan() );
		validateOrderPlacedColumn( (Column) placedBinding.getColumnIterator().next() );

		final Property fulfilledBinding = orderBinding.getProperty( "fulfilled" );
		assertNotNull( fulfilledBinding );
		assertEquals( 1, fulfilledBinding.getColumnSpan() );
		validateOrderFulfilledColumn( (Column) fulfilledBinding.getColumnIterator().next() );

		final Property customerBinding = orderBinding.getProperty( "customer" );
		assertNotNull( customerBinding );
		assertEquals( 1, customerBinding.getColumnSpan() );
		validateOrderCustomerColumn( (Column) customerBinding.getColumnIterator().next() );
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
		validateZipCodePrimaryKeyColumn( (Column) zipCodeBinding.getIdentifier().getColumnIterator().next() );

		final Property codeBinding = zipCodeBinding.getProperty( "code" );
		assertNotNull( codeBinding );
		assertEquals( 1, codeBinding.getColumnSpan() );
		validateZipCodeCodeColumn( (Column) codeBinding.getColumnIterator().next() );

		final Property cityBinding = zipCodeBinding.getProperty( "city" );
		assertNotNull( cityBinding );
		assertEquals( 1, cityBinding.getColumnSpan() );
		validateZipCodeCityColumn( (Column) cityBinding.getColumnIterator().next() );

		final Property stateBinding = zipCodeBinding.getProperty( "state" );
		assertNotNull( stateBinding );
		assertEquals( 1, stateBinding.getColumnSpan() );
		validateZipCodeStateColumn( (Column) stateBinding.getColumnIterator().next() );
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
		validateCustomerRegisteredTrademarksKeyColumn( (Column) collectionBinding.getKey().getColumnIterator().next() );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerRegisteredTrademarksElementColumn(
				(Column) collectionBinding.getElement()
						.getColumnIterator()
						.next()
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
		validateCustomerAddressesKeyColumn( (Column) collectionBinding.getKey().getColumnIterator().next() );

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
		validateCustomerOrdersKeyColumn( (Column) collectionBinding.getKey().getColumnIterator().next() );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerOrdersElementColumn( (Column) collectionBinding.getElement().getColumnIterator().next() );
	}

	protected abstract void validateCustomerOrdersTableName(String name);

	protected abstract void validateCustomerOrdersKeyColumn(Column column);

	protected abstract void validateCustomerOrdersElementColumn(Column column);

	protected void validateCustomerIndustries(Metadata metadata) {
		final Collection collectionBinding = metadata.getCollectionBinding( Customer.class.getName() + ".industries" );
		assertNotNull( collectionBinding );

		validateCustomerIndustriesTableName( collectionBinding.getCollectionTable().getQuotedName() );

		assertEquals( 1, collectionBinding.getKey().getColumnSpan() );
		validateCustomerIndustriesKeyColumn( (Column) collectionBinding.getKey().getColumnIterator().next() );

		assertEquals( 1, collectionBinding.getElement().getColumnSpan() );
		validateCustomerIndustriesElementColumn( (Column) collectionBinding.getElement().getColumnIterator().next() );
	}

	protected abstract void validateCustomerIndustriesTableName(String name);

	protected abstract void validateCustomerIndustriesKeyColumn(Column column);

	protected abstract void validateCustomerIndustriesElementColumn(Column column);
}
