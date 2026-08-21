/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
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
	@SkipForDialect(dialectClass = DB2Dialect.class, matchSubTypes = true)
	@SkipForDialect(dialectClass = DerbyDialect.class)
	public void testTupleReturnWithFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery("insert Mammal (description, bodyWeight, pregnant) values ('Human', 80.0, false)").executeUpdate();
					assertEquals( 1L, session.createSelectionQuery("select count(*) from Mammal", Long.class).getSingleResult() );
					try (ScrollableResults results = session.createQuery("select a, a.bodyWeight from Animal a left join fetch a.offspring", Object[].class).scroll()) {
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
									"select a.description, a.bodyWeight from Animal a inner join fetch a.offspring", Object[].class )
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
							"select a, a.weight from Animal a inner join fetch a.offspring", Object[].class ).scroll()) {
						fail( "scroll allowed with unknown path" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( UnknownPathException.class, e.getCause() );
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, majorVersion = 15, versionMatchMode = VersionMatchMode.SAME_OR_OLDER, reason = "HHH-5229")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingJoinFetchesEmptyResultSet(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					final String query = "from Animal a left join fetch a.offspring where a.description like :desc order by a.id";

					// first, as a control, make sure there are no results
					int size = s.createQuery( query, Animal.class ).setParameter( "desc", "root%" ).list().size();
					assertEquals( 0, size );

					// now get the scrollable results
					try (ScrollableResults results = s.createQuery( query, Animal.class ).setParameter( "desc", "root%" ).scroll()) {

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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
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
											"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
									.setParameter( "desc", "root%" )
									.uniqueResult() );

					try (ScrollableResults results = session
							.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
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
					session.createMutationQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
					session.createMutationQuery( "delete Animal" ).executeUpdate();
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
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingJoinFetchesReverse(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				s -> {
					try (ScrollableResults results = s
							.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingJoinFetchesWithNext(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingNoJoinFetchesWithNext(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a where a.description like :desc order by a.id", Animal.class )
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingJoinFetchesPositioning(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	public void testScrollingNoJoinFetchesPositioning(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session
							.createQuery("from Animal a where a.description like :desc order by a.id", Animal.class )
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
	@JiraKey("HHH-10815")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	@SuppressWarnings("deprecation")
	public void testScrollableResultsGetRowNumber(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					// getRowNumber() numbers the first result 0 (unlike JDBC) and returns -1 when
					// there is no current row. The plain and the fetch-join implementations must
					// report the same row number at the same position (HHH-10815).
					try (
							ScrollableResults<Animal> fetchResult = session.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
									.setParameter( "desc", "root%" ).scroll();
							ScrollableResults<Animal> standardResult = session.createQuery(
									"from Animal a where a.description like :desc order by a.id", Animal.class )
									.setParameter( "desc", "root%" ).scroll()
					) {
						standardResult.first();
						fetchResult.first();
						assertEquals( 0, standardResult.getRowNumber(), "row number must be 0 on the first result" );
						assertEquals( 0, fetchResult.getRowNumber(), "row number must be 0 on the first result" );
						assertEquals( standardResult.getRowNumber(), fetchResult.getRowNumber(),
								"both results must have the same row number" );

						standardResult.next();
						fetchResult.next();
						assertEquals( 1, standardResult.getRowNumber(), "row number must be 1 on the next result" );
						assertEquals( 1, fetchResult.getRowNumber(), "row number must be 1 on the next result" );
						assertEquals( standardResult.getRowNumber(), fetchResult.getRowNumber(),
								"both results must have the same row number" );

						standardResult.last();
						fetchResult.last();
						assertEquals( standardResult.getRowNumber(), fetchResult.getRowNumber(),
								"both results must have the same row number on the last result" );
						assertEquals( standardResult.getPosition() - 1, standardResult.getRowNumber() );
						assertEquals( fetchResult.getPosition() - 1, fetchResult.getRowNumber() );
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-10815")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBackwardsScrollableResultSets.class)
	@SuppressWarnings("deprecation")
	public void testScrollableResultsSetRowNumber(SessionFactoryScope scope) {
		TestData data = new TestData();
		data.prepare( scope );

		scope.inTransaction(
				session -> {
					// setRowNumber(i) must behave identically with and without a fetch join at every
					// position. In particular setRowNumber(0) addresses the position before the first
					// row and must be accepted (returning false), rather than throwing as the
					// fetch-join implementation previously did (HHH-10815).
					try (
							ScrollableResults<Animal> fetchResult = session.createQuery(
									"from Animal a left join fetch a.offspring where a.description like :desc order by a.id", Animal.class )
									.setParameter( "desc", "root%" ).scroll();
							ScrollableResults<Animal> standardResult = session.createQuery(
									"from Animal a where a.description like :desc order by a.id", Animal.class )
									.setParameter( "desc", "root%" ).scroll()
					) {
						// there are two "root%" rows, so positions 1 and 2 are rows and 0 is before the first
						for ( int i = 0; i <= 2; i++ ) {
							assertEquals( standardResult.setRowNumber( i ), fetchResult.setRowNumber( i ),
									"setRowNumber(" + i + ") must return the same result with and without a fetch join" );
							assertEquals( standardResult.getRowNumber(), fetchResult.getRowNumber(),
									"row number at position " + i + " must be the same with and without a fetch join" );
							assertEquals( standardResult.get(), fetchResult.get(),
									"the row at position " + i + " must be the same with and without a fetch join" );
						}
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
						session.createMutationQuery( "delete Animal where description like 'grand%'" ).executeUpdate();
						session.createMutationQuery( "delete Animal where not description like 'root%'" ).executeUpdate();
						session.createMutationQuery( "delete Animal" ).executeUpdate();
					}
			);
		}
	}
}
