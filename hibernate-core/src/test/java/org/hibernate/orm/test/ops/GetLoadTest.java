/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/ops/Node.hbm.xml",
				"org/hibernate/orm/test/ops/Employer.hbm.xml"
		}
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
		}
)
public class GetLoadTest {

	@Test
	public void testGetLoad(SessionFactoryScope scope) {
		clearCounts( scope );

		Employer emp = new Employer();
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		scope.inTransaction(
				session -> {
					session.persist( emp );
					parent.addChild( node );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = session.get( Employer.class, emp.getId() );
					assertTrue( Hibernate.isInitialized( e ) );
					assertFalse( Hibernate.isInitialized( e.getEmployees() ) );
					Node n = session.get( Node.class, node.getName() );
					assertTrue( Hibernate.isInitialized( n ) );
					assertFalse( Hibernate.isInitialized( n.getChildren() ) );
					assertFalse( Hibernate.isInitialized( n.getParent() ) );
					assertNull( session.get( Node.class, "xyz" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = session.getReference( Employer.class, emp.getId() );
					e.getId();
					assertFalse( Hibernate.isInitialized( e ) );
					Node n = session.getReference( Node.class, node.getName() );
					assertThat( n.getName(), is( "foo" ) );
					assertFalse( Hibernate.isInitialized( n ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = (Employer) session.get( "org.hibernate.orm.test.ops.Employer", emp.getId() );
					assertTrue( Hibernate.isInitialized( e ) );
					Node n = (Node) session.get( "org.hibernate.orm.test.ops.Node", node.getName() );
					assertTrue( Hibernate.isInitialized( n ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = (Employer) session.getReference( "org.hibernate.orm.test.ops.Employer", emp.getId() );
					e.getId();
					assertFalse( Hibernate.isInitialized( e ) );
					Node n = (Node) session.getReference( "org.hibernate.orm.test.ops.Node", node.getName() );
					assertThat( n.getName(), is( "foo" ) );
					assertFalse( Hibernate.isInitialized( n ) );
				}
		);

		assertFetchCount( 0, scope );
	}

	@Test
	public void testGetAfterDelete(SessionFactoryScope scope) {
		clearCounts( scope );

		Employer emp = new Employer();

		scope.inTransaction(
				session ->
						session.persist( emp )
		);

		Employer e = scope.fromTransaction(
				session -> {
					session.remove( emp );
					return session.get( Employer.class, emp.getId() );
				}
		);

		assertNull( e, "get did not return null after delete" );
	}

	private void clearCounts(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
	}

	private void assertFetchCount(int count, SessionFactoryScope scope) {
		int fetches = (int) scope.getSessionFactory().getStatistics().getEntityFetchCount();
		assertThat( fetches, is( count ) );
	}

}
