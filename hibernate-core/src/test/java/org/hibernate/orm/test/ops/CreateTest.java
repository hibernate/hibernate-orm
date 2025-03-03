/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;

import org.hibernate.dialect.HANADialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
public class CreateTest extends AbstractOperationTestCase {

	@Test
	@SuppressWarnings("unchecked")
	public void testNoUpdatesOnCreateVersionedWithCollection(SessionFactoryScope scope) {
		clearCounts( scope );

		VersionedEntity root = new VersionedEntity( "root", "root" );
		scope.inTransaction(
				session -> {
					VersionedEntity child = new VersionedEntity( "c1", "child-1" );
					root.getChildren().add( child );
					child.setParent( root );
					session.persist( root );
				}
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );
		assertDeleteCount( 0, scope );

		scope.inTransaction(
				session ->
						session.remove( root )
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testCreateTree(SessionFactoryScope scope) {
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					Node root = new Node( "root" );
					Node child = new Node( "child" );
					root.addChild( child );
					session.persist( root );
				}
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );

		scope.inTransaction(
				session -> {
					Node root = session.get( Node.class, "root" );
					Node child2 = new Node( "child2" );
					root.addChild( child2 );
				}
		);

		assertInsertCount( 3, scope );
		assertUpdateCount( 0, scope );
	}

	@Test
	public void testCreateTreeWithGeneratedId(SessionFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		scope.inTransaction(
				session -> {
					NumberedNode child = new NumberedNode( "child" );
					root.addChild( child );
					session.persist( root );
				}
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );

		scope.inTransaction(
				session -> {
					NumberedNode r = session.get( NumberedNode.class, root.getId() );
					NumberedNode child2 = new NumberedNode( "child2" );
					r.addChild( child2 );
				}
		);

		assertInsertCount( 3, scope );
		assertUpdateCount( 0, scope );
	}

	@Test
	public void testCreateException(SessionFactoryScope scope) {
		Node dupe = new Node( "dupe" );
		scope.inTransaction(
				session -> {
					session.persist( dupe );
					session.persist( dupe );
				}
		);

		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						session.persist( dupe );
						session.getTransaction().commit();
						fail( "Expecting constraint failure" );
					}
					catch (PersistenceException e) {

						//verify that an exception is thrown!
						assertTyping( ConstraintViolationException.class, e );
					}
					finally {
						if(session.getTransaction().isActive()){
							session.getTransaction().rollback();
						}
					}
				}
		);

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						session.persist( nondupe );
						session.getTransaction().commit();
						fail( "Expecting constraint failure" );
					}
					catch (PersistenceException e) {
						//verify that an exception is thrown!
						assertTyping( ConstraintViolationException.class, e );
					}
					finally {
						if(session.getTransaction().isActive()){
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testCreateExceptionWithGeneratedId(SessionFactoryScope scope) {
		NumberedNode dupe = new NumberedNode( "dupe" );
		scope.inTransaction(
				session -> {
					session.persist( dupe );
					session.persist( dupe );
				}
		);

		scope.inTransaction(
				session -> {
					try {
						session.persist( dupe );
						fail( "Expecting failure" );
					}
					catch (Exception e) {
						//verify that an exception is thrown!
						assertTyping( EntityExistsException.class, e );
					}
				}
		);

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		scope.inTransaction(
				session -> {
					try {
						session.persist( nondupe );
						fail( "Expecting failure" );
					}
					catch (Exception e) {
						//verify that an exception is thrown!
						assertTyping( EntityExistsException.class, e );
					}
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	@SkipForDialect(dialectClass = HANADialect.class, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testBasic(SessionFactoryScope scope) throws Exception {
		Employer er = new Employer();
		Employee ee = new Employee();
		scope.inTransaction(
				session -> {
					session.persist( ee );
					Collection erColl = new ArrayList();
					Collection eeColl = new ArrayList();
					erColl.add( ee );
					eeColl.add( er );
					er.setEmployees( erColl );
					ee.setEmployers( eeColl );
				}
		);

		scope.inTransaction(
				session -> {
					Employer er1 = session.getReference( Employer.class, er.getId() );
					assertNotNull( er1 );
					assertNotNull( er1.getEmployees() );
					assertThat( er1.getEmployees().size(), is( 1 ) );
					Employee eeFromDb = (Employee) er1.getEmployees().iterator().next();
					assertThat( eeFromDb.getId(), is( ee.getId() ) );
				}
		);
	}
}
