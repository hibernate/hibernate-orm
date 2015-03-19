/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.namingstrategy.complete;

import java.util.Iterator;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Selectable;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * The default naming strategy is actually always EJB3NamingStrategy historically,
 * even when binding hbm documents.  So test that combo.
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-9633" )
public class LegacyJpaNamingWithHbmBindingTests extends BaseHbmBindingTests {
	@Override
	protected void applyFixtures(Configuration cfg) {
		cfg.setNamingStrategy( EJB3NamingStrategy.INSTANCE );
		super.applyFixtures( cfg );
	}

	@Override
	protected void validateCustomerPrimaryTableName(String name) {
		assertEquals( "Customer", name );
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
				assertEquals( "zipCode", column.getQuotedName() );
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
		assertEquals( "customer", column.getQuotedName() );
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
		assertEquals( "Customer_registeredTrademarks", name );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerRegisteredTrademarksElementColumn(Column column) {
		assertEquals( "elt", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerAddressesTableName(String name) {
		assertEquals( "Customer_addresses", name );
	}

	@Override
	protected void validateCustomerAddressesKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
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
				assertEquals( "zipCode", column.getQuotedName() );
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
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerOrdersElementColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerIndustriesTableName(String name) {
		assertEquals( "Customer_industries", name );
	}

	@Override
	protected void validateCustomerIndustriesKeyColumn(Column column) {
		assertEquals( "id", column.getQuotedName() );
	}

	@Override
	protected void validateCustomerIndustriesElementColumn(Column column) {
		assertEquals( "elt", column.getQuotedName() );
	}
}
