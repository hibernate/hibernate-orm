/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hqlfetchscroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/hqlfetchscroll/ParentChild.hbm.xml"
)
@SessionFactory
public class HQLScrollFetchTest {
	private static final String QUERY = "select p from Parent p join fetch p.children c";

	@Test
	public void testNoScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List list = session.createQuery( QUERY, Parent.class )
							.setTupleTransformer( (tuple, aliases) -> tuple[0] )
							.setResultListTransformer( resultList -> Arrays.asList( new HashSet(resultList).toArray() ) )
							.list();
					assertResultFromAllUsers( list );
				}
		);
	}

	@Test
	public void testScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name asc", Parent.class )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	public void testIncompleteScrollFirstResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testIncompleteScrollSecondResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.next();
						p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	public void testIncompleteScrollFirstResultInTransaction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testIncompleteScrollSecondResultInTransaction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.next();
						p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testIncompleteScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						// get the other parent entity from the persistence context along with its first child
						// retrieved from the resultset.
						Parent pOther = null;
						Child cOther = null;
						for ( Object entity : session.getPersistenceContext().getEntitiesByKey().values() ) {
							if ( Parent.class.isInstance( entity ) ) {
								if ( entity != p ) {
									if ( pOther != null ) {
										fail( "unexpected parent found." );
									}
									pOther = (Parent) entity;
								}
							}
							else if ( Child.class.isInstance( entity ) ) {
								if ( !p.getChildren().contains( entity ) ) {
									if ( cOther != null ) {
										fail( "unexpected child entity found" );
									}
									cOther = (Child) entity;
								}
							}
							else {
								fail( "unexpected type of entity." );
							}
						}
						// check that the same second parent is obtained by calling Session.get()
						assertNull( pOther );
						assertNull( cOther );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testIncompleteScrollLast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.last();
						// get the other parent entity from the persistence context.
						// since the result set was scrolled to the end, the other parent entity's collection has been
						// properly initialized.
						Parent pOther = null;
						Set childrenOther = new HashSet();
						for ( Object entity : session.getPersistenceContext().getEntitiesByKey().values() ) {
							if ( Parent.class.isInstance( entity ) ) {
								if ( entity != p ) {
									if ( pOther != null ) {
										fail( "unexpected parent found." );
									}
									pOther = (Parent) entity;
								}
							}
							else if ( Child.class.isInstance( entity ) ) {
								if ( !p.getChildren().contains( entity ) ) {
									childrenOther.add( entity );
								}
							}
							else {
								fail( "unexpected type of entity." );
							}
						}
						// check that the same second parent is obtained by calling Session.get()
						assertNotNull( pOther );
						assertSame( pOther, session.get( Parent.class, pOther.getId() ) );
						// access pOther's collection; should be completely loaded
						assertTrue( Hibernate.isInitialized( pOther.getChildren() ) );
						assertEquals( childrenOther, pOther.getChildren() );
						assertResultFromOneUser( pOther );
					}

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testScrollOrderParentAsc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc", Parent.class ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testScrollOrderParentDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name desc", Parent.class ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testScrollOrderParentAscChildrenAsc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name asc", Parent.class )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1283")
	public void testScrollOrderParentAscChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name desc", Parent.class )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	final String errMsg = "should have failed because data is ordered incorrectly.";

	@Test
	public void testScrollOrderChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p0 = new Parent( "parent0" );
					session.persist( p0 );
				}
		);

		scope.inSession(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by c.name desc", Parent.class ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}

						try {
							assertResultFromAllUsers( list );
							fail( errMsg );
						}
						catch (AssertionError ex) {
							if ( errMsg.equalsIgnoreCase( ex.getMessage() ) ) {
								throw ex;
							}
							// Other AssertionErrors expected
						}
					}
				}
		);
	}

	@Test
	public void testListOrderChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p0 = new Parent( "parent0" );
					session.persist( p0 );
				}
		);

		scope.inSession(
				session -> {
					List results = session.createQuery( QUERY + " order by c.name desc", Parent.class ).list();
//					try {
						assertResultFromAllUsers( results );
//						fail( errMsg );
//					}
//					catch (AssertionError ex) {
//						if ( errMsg.equalsIgnoreCase( ex.getMessage() ) ) {
//							throw ex;
//						}
//						// Other AssertionErrors expected
//					}
				}
		);
	}

	@Test
	@JiraKey("HHH-10815")
	public void testScrollableResultsGetRowNumber(SessionFactoryScope scope) {
		createFiveParentsWithNoChild( scope );

		scope.inTransaction(
				s -> {
					try (
							ScrollableResults<Parent> fetchResult = s.createQuery(
									"from Parent p left join fetch p.children order by p.id",
									Parent.class
							).scroll();

							ScrollableResults<Parent> standardResult = s.createQuery(
									"from Parent p order by p.id",
									Parent.class
							).scroll()
					) {
						standardResult.first();
						fetchResult.first();

						assertEquals(
								0,
								standardResult.getRowNumber(),
								"row number must be 0 on first result"
						);
						assertEquals(
								0,
								fetchResult.getRowNumber(),
								"row number must be 0 on first result"
						);

						standardResult.next();
						fetchResult.next();

						assertEquals(
								1,
								standardResult.getRowNumber(),
								"row number must be 1 on next result"
						);
						assertEquals(
								1,
								fetchResult.getRowNumber(),
								"row number must be 1 on next result"
						);

						standardResult.last();
						fetchResult.last();

						assertEquals(
								standardResult.getRowNumber(),
								fetchResult.getRowNumber(),
								"Both results should have the same row number"
						);
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-10815")
	public void testScrollableResultsSetRowNumber(SessionFactoryScope scope) {
		createFiveParentsWithNoChild( scope );

		scope.inTransaction(
				s -> {
					try (
							ScrollableResults<Parent> fetchResult = s.createQuery(
									"from Parent p left join fetch p.children order by p.id",
									Parent.class
							).scroll();

							ScrollableResults<Parent> standardResult = s.createQuery(
									"from Parent p order by p.id",
									Parent.class
							).scroll()
					) {
						for ( int rowNumber = 0; rowNumber < 5; rowNumber++ ) {
							assertTrue( standardResult.setRowNumber( rowNumber ) );
							assertTrue( fetchResult.setRowNumber( rowNumber ) );

							assertEquals(
									standardResult.get(),
									fetchResult.get(),
									"Both results should point to the same row"
							);

							assertEquals(
									rowNumber,
									standardResult.getRowNumber()
							);

							assertEquals(
									rowNumber,
									fetchResult.getRowNumber()
							);
						}
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-10815")
	public void testScrollableResultsSetNegativeRowNumber(SessionFactoryScope scope) {
		createFiveParentsWithNoChild( scope );

		scope.inTransaction(
				s -> {
					try (
							ScrollableResults<Parent> fetchResult = s.createQuery(
									"from Parent p left join fetch p.children order by p.id",
									Parent.class
							).scroll();

							ScrollableResults<Parent> standardResult = s.createQuery(
									"from Parent p order by p.id",
									Parent.class
							).scroll()
					) {
						assertTrue( standardResult.setRowNumber( -1 ) );
						assertTrue( fetchResult.setRowNumber( -1 ) );

						assertEquals(
								standardResult.get(),
								fetchResult.get()
						);

						assertEquals(
								4,
								standardResult.getRowNumber()
						);

						assertEquals(
								4,
								fetchResult.getRowNumber()
						);

						assertTrue( standardResult.setRowNumber( -2 ) );
						assertTrue( fetchResult.setRowNumber( -2 ) );

						assertEquals(
								standardResult.get(),
								fetchResult.get()
						);

						assertEquals(
								3,
								standardResult.getRowNumber()
						);

						assertEquals(
								3,
								fetchResult.getRowNumber()
						);
					}
				}
		);
	}

	private void createFiveParentsWithNoChild(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();

		scope.inTransaction(
				s -> {
					for ( int i = 0; i < 5; i++ ) {
						Parent p = new Parent( "parent" + i );
						s.persist( p );
					}
				}
		);
	}

	private void assertResultFromOneUser(Parent parent) {
		assertEquals(
				3,
				parent.getChildren().size(),
				"parent " + parent + " has incorrect collection(" + parent.getChildren() + ")."
		);
	}

	private void assertResultFromAllUsers(List list) {
		assertEquals( 2, list.size(), "list is not correct size: " );
		for ( Object aList : list ) {
			assertResultFromOneUser( (Parent) aList );
		}
	}

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child_1_1 = new Child( "achild1-1" );
					Child child_1_2 = new Child( "ychild1-2" );
					Child child_1_3 = new Child( "dchild1-3" );
					Child child_2_1 = new Child( "bchild2-1" );
					Child child_2_2 = new Child( "cchild2-2" );
					Child child_2_3 = new Child( "zchild2-3" );

					session.persist( child_1_1 );
					session.persist( child_2_1 );
					session.persist( child_1_2 );
					session.persist( child_2_2 );
					session.persist( child_1_3 );
					session.persist( child_2_3 );

					session.flush();

					Parent p1 = new Parent( "parent1" );
					p1.addChild( child_1_1 );
					p1.addChild( child_1_2 );
					p1.addChild( child_1_3 );
					session.persist( p1 );

					Parent p2 = new Parent( "parent2" );
					p2.addChild( child_2_1 );
					p2.addChild( child_2_2 );
					p2.addChild( child_2_3 );
					session.persist( p2 );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
