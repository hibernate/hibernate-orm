/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import java.util.Iterator;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Selectable;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class LegacyJpaNamingWithAnnotationBindingTests extends BaseAnnotationBindingTests {
	@Override
	protected ImplicitNamingStrategy getImplicitNamingStrategyToUse() {
		return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE;
	}

	@Override
	protected void validateCustomerPrimaryTableName(String name) {
		assertEquals( "CuStOmEr", name );
	}

	@Override
	protected void validateCustomerPrimaryKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerVersionColumn(Column column) {
		assertEquals( "version", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerNameColumn(Column column) {
		assertEquals( "name", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerHqAddressComponent(Component component) {
		assertEquals( 3, component.getColumnSpan() );
		Iterator<Selectable> selectables = component.getColumnIterator();
		int pass = 1;
		while ( selectables.hasNext() ) {
			final Column column = assertTyping( Column.class, selectables.next() );
			if ( pass == 1 ) {
				assertEquals( "line1", column.getQuotedName() );
			}
			else if ( pass == 2 ) {
				assertEquals( "line2", column.getQuotedName() );
			}
			else if ( pass == 3 ) {
				assertEquals( "zipCode_id", column.getQuotedName() );
			}
			pass++;
		}
	}

	@Override
	protected void validateOrderPrimaryTableName(String name) {
		assertEquals( "Order", name );
	}

	@Override
	protected void validateOrderPrimaryKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateOrderReferenceCodeColumn(Column column) {
		assertEquals( "referenceCode", column.getQuotedName() );
	}

	@Override
	protected void validateOrderFulfilledColumn(Column column) {
		assertEquals( "fulfilled", column.getQuotedName() );
	}

	@Override
	protected void validateOrderPlacedColumn(Column column) {
		assertEquals( "placed", column.getQuotedName() );
	}

	@Override
	protected void validateOrderCustomerColumn(Column column) {
		assertEquals( "customer_id", column.getQuotedName() );
	}

	@Override
	protected void validateZipCodePrimaryTableName(String name) {
		assertEquals( "ZipCode", name );
	}

	@Override
	protected void validateZipCodePrimaryKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateZipCodeCodeColumn(Column column) {
		assertEquals( "code", column.getQuotedName() );
	}

	@Override
	protected void validateZipCodeCityColumn(Column column) {
		assertEquals( "city", column.getQuotedName() );
	}

	@Override
	protected void validateZipCodeStateColumn(Column column) {
		assertEquals( "state", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksTableName(String name) {
		// CuStOmEr is the Customer entity primary table name (implicitly via jpa entity name)
		assertEquals( "CuStOmEr_registeredTrademarks", name );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksKeyColumn(Column column) {
		// CuStOmEr is the Customer entity primary table name (implicitly via jpa entity name)
		assertEquals( "CuStOmEr_id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksElementColumn(Column column) {
		assertEquals( "registeredTrademarks", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerAddressesTableName(String name) {
		// CuStOmEr is the Customer entity primary table name (implicitly via jpa entity name)
		assertEquals( "CuStOmEr_addresses", name );
	}

	@Override
	protected void validateCustomerAddressesKeyColumn(Column column) {
		// CuStOmEr is the Customer entity primary table name (implicitly via jpa entity name)
		assertEquals( "CuStOmEr_id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerAddressesElementComponent(Component component) {
		assertEquals( 3, component.getColumnSpan() );
		Iterator<Selectable> selectables = component.getColumnIterator();
		int pass = 1;
		while ( selectables.hasNext() ) {
			final Column column = assertTyping( Column.class, selectables.next() );
			if ( pass == 1 ) {
				assertEquals( "line1", column.getQuotedName() );
			}
			else if ( pass == 2 ) {
				assertEquals( "line2", column.getQuotedName() );
			}
			else if ( pass == 3 ) {
				assertEquals( "zipCode_id", column.getQuotedName() );
			}
			pass++;
		}
	}

	@Override
	protected void validateCustomerOrdersTableName(String name) {
		assertEquals( "Order", name );
	}

	@Override
	protected void validateCustomerOrdersKeyColumn(Column column) {
		assertEquals( "customer_id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerOrdersElementColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerIndustriesTableName(String name) {
		// CuStOmEr is the Customer entity primary table name (implicitly via jpa entity name)
		assertEquals( "CuStOmEr_InDuStRy", name );
	}

	@Override
	protected void validateCustomerIndustriesKeyColumn(Column column) {
		assertEquals( "CuStOmEr_id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerIndustriesElementColumn(Column column) {
		assertEquals( "industries_id", column.getQuotedName() );
	}
}
