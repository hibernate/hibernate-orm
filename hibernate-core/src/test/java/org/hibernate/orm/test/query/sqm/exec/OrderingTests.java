/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.exec;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests for order-by clauses
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class OrderingTests {
	@Test
	public void testBasicOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p order by p.name.familiarName" )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-1356" )
	public void testFunctionBasedOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p order by upper( p.name.familiarName )" )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-11688" )
	public void testSelectAliasOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select v.name as n from Vendor v order by n" )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-11688" )
	public void testSelectPositionOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select v.name as n from Vendor v order by 1" )
							.list();
				}
		);
	}
}
