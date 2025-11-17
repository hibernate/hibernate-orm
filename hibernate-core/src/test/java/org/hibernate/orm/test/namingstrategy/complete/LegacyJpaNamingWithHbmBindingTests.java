/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Selectable;
import org.hibernate.testing.orm.junit.JiraKey;

import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

/**
 * The default naming strategy is actually always EJB3NamingStrategy historically,
 * even when binding hbm documents.  So test that combo.
 *
 * @author Steve Ebersole
 */
@JiraKey(value = "")
public class LegacyJpaNamingWithHbmBindingTests extends BaseHbmBindingTests {
	@Override
	protected ImplicitNamingStrategy getImplicitNamingStrategyToUse() {
		return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE;
	}

	@Override
	protected void validateCustomerPrimaryTableName(String name) {
		assertThat( name, equalTo( "Customer" ) );
	}

	@Override
	protected void validateCustomerPrimaryKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerVersionColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "version" ) );
	}

	@Override
	protected void validateCustomerNameColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "name" ) );
	}

	@Override
	protected void validateCustomerHqAddressComponent(Component component) {
		assertThat( component.getColumnSpan(), equalTo( 3 ) );
		Iterator<Selectable> selectables = component.getSelectables().iterator();
		int pass = 1;
		while ( selectables.hasNext() ) {
			final Column column = assertTyping( Column.class, selectables.next() );
			if ( pass == 1 ) {
				assertThat( column.getQuotedName(), equalTo( "line1" ) );
			}
			else if ( pass == 2 ) {
				assertThat( column.getQuotedName(), equalTo( "line2" ) );
			}
			else if ( pass == 3 ) {
				assertThat( column.getQuotedName(), equalTo( "zipCode" ) );
			}
			pass++;
		}
	}

	@Override
	protected void validateOrderPrimaryTableName(String name) {
		assertThat( name, anyOf( equalTo( "Order" ), equalTo( "`Order`" ) ) );
	}

	@Override
	protected void validateOrderPrimaryKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateOrderReferenceCodeColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "referenceCode" ) );
	}

	@Override
	protected void validateOrderFulfilledColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "fulfilled" ) );
	}

	@Override
	protected void validateOrderPlacedColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "placed" ) );
	}

	@Override
	protected void validateOrderCustomerColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "customer" ) );
	}

	@Override
	protected void validateZipCodePrimaryTableName(String name) {
		assertThat( name, equalTo( "ZipCode" ) );
	}

	@Override
	protected void validateZipCodePrimaryKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateZipCodeCodeColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "code" ) );
	}

	@Override
	protected void validateZipCodeCityColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "city" ) );
	}

	@Override
	protected void validateZipCodeStateColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "state" ) );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksTableName(String name) {
		assertThat( name, equalTo( "Customer_registeredTrademarks" ) );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksElementColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "elt" ) );
	}

	@Override
	protected void validateCustomerAddressesTableName(String name) {
		assertThat( name, equalTo( "Customer_addresses" ) );
	}

	@Override
	protected void validateCustomerAddressesKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerAddressesElementComponent(Component component) {
		assertThat( component.getColumnSpan(), equalTo( 3 ) );
		Iterator<Selectable> selectables = component.getSelectables().iterator();
		int pass = 1;
		while ( selectables.hasNext() ) {
			final Column column = assertTyping( Column.class, selectables.next() );
			if ( pass == 1 ) {
				assertThat( column.getQuotedName(), equalTo( "line1" ) );
			}
			else if ( pass == 2 ) {
				assertThat( column.getQuotedName(), equalTo( "line2" ) );
			}
			else if ( pass == 3 ) {
				assertThat( column.getQuotedName(), equalTo( "zipCode" ) );
			}
			pass++;
		}
	}

	@Override
	protected void validateCustomerOrdersTableName(String name) {
		assertThat( name, anyOf( equalTo( "Order" ), equalTo( "`Order`" ) ) );
	}

	@Override
	protected void validateCustomerOrdersKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerOrdersElementColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerIndustriesTableName(String name) {
		assertThat( name, equalTo( "Customer_industries" ) );
	}

	@Override
	protected void validateCustomerIndustriesKeyColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "id" ) );
	}

	@Override
	protected void validateCustomerIndustriesElementColumn(Column column) {
		assertThat( column.getQuotedName(), equalTo( "elt" ) );
	}
}
