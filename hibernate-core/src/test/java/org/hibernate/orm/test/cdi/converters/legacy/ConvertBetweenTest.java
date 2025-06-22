/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests using converters with the between construction.
 *
 * @author Etienne Miret
 */
public class ConvertBetweenTest extends AbstractJPATest {

	@Override
	public String[] getOrmXmlFiles() {
		return new String[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@BeforeEach
	public void fillData() {
		inTransaction(
				session -> {
					final Item i0 = new Item();
					i0.setPrice( new BigDecimal( "12.05" ) );
					i0.setQuantity( 10 );
					session.persist( i0 );

					final Item i1 = new Item();
					i1.setPrice( new BigDecimal( "5.35" ) );
					i1.setQuantity( 5 );
					session.persist( i1 );

					final Item i2 = new Item();
					i2.setPrice( new BigDecimal( "99.99" ) );
					i2.setQuantity( 15 );
					session.persist( i2 );
				}
		);
	}

	@AfterEach
	public void cleanUpData() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-9356")
	public void testBetweenLiteral() {
		inTransaction(
				session -> {
					@SuppressWarnings("unchecked") final List<Item> result = session.createQuery(
							"select i from Item i where quantity between 9 and 11" ).list();
					assertEquals( 1, result.size() );
					assertEquals( 10, result.get( 0 ).getQuantity().intValue() );
				}
		);
	}

	@Test
	public void testBetweenParameters() {
		inTransaction(
				session -> {
					final Query query = session.createQuery(
							"select i from Item i where quantity between :low and :high" );
					query.setParameter( "low", 9 );
					query.setParameter( "high", 11 );
					@SuppressWarnings("unchecked") final List<Item> result = query.list();
					assertEquals( 1, result.size() );
					assertEquals( 10, result.get( 0 ).getQuantity().intValue() );
				}
		);
	}

}
