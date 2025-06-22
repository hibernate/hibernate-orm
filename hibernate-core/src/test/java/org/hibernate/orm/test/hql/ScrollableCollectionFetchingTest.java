/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests the new functionality of allowing scrolling of results which
 * contain collection fetches.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/hql/Animal.hbm.xml"
)
@SessionFactory
public class ScrollableCollectionFetchingTest {

	@Test
	@SkipForDialect(dialectClass=DB2Dialect.class, matchSubTypes = true)
	@SkipForDialect(dialectClass= DerbyDialect.class)
	public void testTupleReturnWithFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery("insert Mammal (description, bodyWeight, pregnant) values ('Human', 80.0, false)").executeUpdate();
					assertEquals( 1L, session.createSelectionQuery("select count(*) from Mammal").getSingleResult() );
					try (ScrollableResults results = session.createQuery("select a, a.bodyWeight from Animal a left join fetch a.offspring").scroll()) {
						assertTrue( results.next() );
						Object[] result = (Object[]) results.get();
						assertTrue( Hibernate.isInitialized( ( (Animal) result[0] ).getOffspring() ) );
						session.createMutationQuery( "delete Mammal" ).executeUpdate();
					}
				}
		);
	}

	@Test
	public void testTupleReturnWithFetchFailure(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults<?> sr = session.createQuery(
									"select a.description, a.bodyWeight from Animal a inner join fetch a.offspring" )
							.scroll()) {
						fail( "scroll allowed with fetch and projection result" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( SemanticException.class, e.getCause() );
					}
				}
		);
	}


	@Test
	public void testUknownPathFailure(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults<?> sr = session.createQuery(
							"select a, a.weight from Animal a inner join fetch a.offspring" ).scroll()) {
						fail( "scroll allowed with unknown path" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( UnknownPathException.class, e.getCause() );
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, majorVersion = 15, matchSubTypes = true, reason = "HHH-5229")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingJoinFetchesEmptyResultSet(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					final String query = "from Animal a left join fetch a.offspring where a.description like :desc order by a.id";

					// first, as a control, make sure there are no results
					int size = s.createQuery( query ).setParameter( "desc", "root%" ).list().size();
					assertEquals( 0, size );

					// now get the scrollable results
					try (ScrollableResults results = s.createQuery( query ).setParameter( "desc", "root%" ).scroll()) {

						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );

						assertFalse( results.next() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );

						assertFalse( results.previous() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );

						results.beforeFirst();
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.next() );

						assertFalse( results.first() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.next() );

						results.afterLast();
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.next() );

						assertFalse( results.last() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.next() );

						for ( int i = 1; i < 3; i++ ) {
							assertFalse( results.scroll( i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );

							assertFalse( results.scroll( -i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );

							assertFalse( results.setRowNumber( i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );

							assertFalse( results.setRowNumber( -i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );
						}
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors")
	public void testScrollingJoinFetchesSingleRowResultSet(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Animal mother = new Animal();
					mother.setDescription( "root-1" );

					Animal daughter = new Animal();
					daughter.setDescription( "daughter" );

					daughter.setMother( mother );
					mother.addOffspring( daughter );

					session.persist( mother );
					session.persist( daughter );
				}
		);

		scope.inTransaction(
				session -> {
					assertNotNull(
							session
									.createQuery(
											"from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
									.setParameter( "desc", "root%" )
									.uniqueResult() );

					try (ScrollableResults results = session
							.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" ).scroll()) {

						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.previous() );

						assertTrue( results.next() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );

						assertFalse( results.next() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );

						assertTrue( results.previous() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );

						assertFalse( results.previous() );
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );

						assertTrue( results.next() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );

						results.beforeFirst();
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.previous() );

						assertTrue( results.first() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );
						assertFalse( results.next() );

						results.afterLast();
						assertFalse( results.isFirst() );
						assertFalse( results.isLast() );
						assertFalse( results.next() );

						assertTrue( results.last() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );
						assertFalse( results.next() );

						assertTrue( results.first() );
						assertTrue( results.isFirst() );
						assertTrue( results.isLast() );

						for ( int i = 1; i < 3; i++ ) {
							assertTrue( results.setRowNumber( 1 ) );
							assertTrue( results.isFirst() );
							assertTrue( results.isLast() );

							assertFalse( results.scroll( i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );

							assertTrue( results.setRowNumber( 1 ) );
							assertTrue( results.isFirst() );
							assertTrue( results.isLast() );

							assertFalse( results.scroll( -i ) );
							assertFalse( results.isFirst() );
							assertFalse( results.isLast() );

							if ( i != 1 ) {
								assertFalse( results.setRowNumber( i ) );
								assertFalse( results.isFirst() );
								assertFalse( results.isLast() );

								assertFalse( results.setRowNumber( -i ) );
								assertFalse( results.isFirst() );
								assertFalse( results.isLast() );
							}
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
					session.createQuery( "delete Animal" ).executeUpdate();
				}
		);
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportsResultSetPositioningOnForwardOnlyCursorCheck.class,
			comment = "Driver does not support result set positioning  methods on forward-only cursors"
	)
	public void testScrollingJoinFetchesForward(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				s -> {
					try (ScrollableResults results = s
							.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" )
							.scroll( ScrollMode.FORWARD_ONLY )) {

						int counter = 0;
						while ( results.next() ) {
							counter++;
							Animal animal = (Animal) results.get();
							checkResult( animal );
						}
						assertEquals( 2, counter, "unexpected result count" );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingJoinFetchesReverse(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				s -> {
					try (ScrollableResults results = s
							.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" ).scroll()) {

						results.afterLast();

						int counter = 0;
						while ( results.previous() ) {
							counter++;
							Animal animal = (Animal) results.get();
							checkResult( animal );
						}
						assertEquals( 2, counter, "unexpected result count" );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingJoinFetchesWithNext(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" )
							.scroll()) {

						assertEquals( 0, results.getPosition() );

						assertTrue( results.next() );
						Animal animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 1, results.getPosition() );

						assertTrue( results.next() );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 2, results.getPosition() );

						assertFalse( results.next() );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingNoJoinFetchesWithNext(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" )
							.scroll()) {

						assertEquals( 0, results.getPosition() );

						assertTrue( results.next() );
						Animal animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 1, results.getPosition() );

						assertTrue( results.next() );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 2, results.getPosition() );

						assertFalse( results.next() );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingJoinFetchesPositioning(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a left join fetch a.offspring where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" )
							.scroll()) {

						results.first();
						Animal animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "first() did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.scroll( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "scroll(1) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.scroll( -1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "scroll(-1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.next();
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.setRowNumber( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "setRowNumber(1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.setRowNumber( 2 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "setRowNumber(2) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.setRowNumber( -2 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "setRowNumber(-2) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.setRowNumber( -1 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "setRowNumber(-1) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.position( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "position(1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.position( 2 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "position(2) did not return expected row" );
						assertEquals( 2, results.getPosition() );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA only supports forward-only cursors.")
	public void testScrollingNoJoinFetchesPositioning(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a where a.description like :desc order by a.id" )
							.setParameter( "desc", "root%" )
							.scroll()) {

						results.first();
						Animal animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "first() did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.scroll( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "scroll(1) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.scroll( -1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "scroll(-1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.next();
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "next() did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.setRowNumber( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "setRowNumber(1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.setRowNumber( 2 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "setRowNumber(2) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.setRowNumber( -2 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "setRowNumber(-2) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.setRowNumber( -1 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "setRowNumber(-1) did not return expected row" );
						assertEquals( 2, results.getPosition() );

						results.position( 1 );
						animal = (Animal) results.get();
						assertEquals( data.root1Id, animal.getId(), "position(1) did not return expected row" );
						assertEquals( 1, results.getPosition() );

						results.position( 2 );
						animal = (Animal) results.get();
						assertEquals( data.root2Id, animal.getId(), "position(2) did not return expected row" );
						assertEquals( 2, results.getPosition() );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		TestData.cleanup( scope );
	}

	private void checkResult(Animal animal) {
		if ( "root-1".equals( animal.getDescription() ) ) {
			assertEquals( 2, animal.getOffspring().size(), "root-1 did not contain both children" );
		}
		else if ( "root-2".equals( animal.getDescription() ) ) {
			assertEquals( 0, animal.getOffspring().size(), "root-2 did not contain zero children" );
		}
	}

	private static class TestData {

		private Long root1Id;
		private Long root2Id;

		private void prepare(SessionFactoryScope scope) {
			Animal mother = new Animal();
			Animal another = new Animal();
			scope.inTransaction(
					session -> {
						mother.setDescription( "root-1" );

						another.setDescription( "root-2" );

						Animal son = new Animal();
						son.setDescription( "son" );

						Animal daughter = new Animal();
						daughter.setDescription( "daughter" );

						Animal grandson = new Animal();
						grandson.setDescription( "grandson" );

						Animal grandDaughter = new Animal();
						grandDaughter.setDescription( "granddaughter" );

						son.setMother( mother );
						mother.addOffspring( son );

						daughter.setMother( mother );
						mother.addOffspring( daughter );

						grandson.setMother( daughter );
						daughter.addOffspring( grandson );

						grandDaughter.setMother( daughter );
						daughter.addOffspring( grandDaughter );

						session.persist( mother );
						session.persist( another );
						session.persist( son );
						session.persist( daughter );
						session.persist( grandson );
						session.persist( grandDaughter );
					}
			);

			root1Id = mother.getId();
			root2Id = another.getId();
		}

		public static void cleanup(SessionFactoryScope scope) {
			scope.inTransaction(
					session -> {
						session.createQuery( "delete Animal where description like 'grand%'" ).executeUpdate();
						session.createQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
						session.createQuery( "delete Animal" ).executeUpdate();
					}
			);
		}
	}
}
