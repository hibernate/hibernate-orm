/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

/**
 * Tests for order-by clauses
 * @author Steve Ebersole
 */
public class OrderingTests extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.RETAIL.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testBasicOrdering() {
		inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p order by p.name.familiarName" )
							.list();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1356" )
	public void testFunctionBasedOrdering() {
		inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p order by upper( p.name.familiarName )" )
							.list();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11688" )
	public void testSelectAliasOrdering() {
		inTransaction(
				session -> {
					session.createQuery( "select v.name as n from Vendor v order by n" )
							.list();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11688" )
	public void testSelectPositionOrdering() {
		inTransaction(
				session -> {
					session.createQuery( "select v.name as n from Vendor v order by 1" )
							.list();
				}
		);
	}
}
