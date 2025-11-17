/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.domain.gambit.EntityWithAggregateId;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@SessionFactoryFunctionalTesting
public class MultiIdEntityLoadTests {
	@Test
	public void testBasicEntitySimpleLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class ).multiLoad( 1, 3 );
					assertThat( results.size(), is( 2 ) );
				}
		);
	}

	@Test
	@JiraKey( "HHH-17201" )
	public void testSimpleEntityUnorderedMultiLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Integer> idList = List.of( 0, 1 );
					session.byMultipleIds( SimpleEntity.class )
							.enableOrderedReturn( false ).multiLoad( idList );
				}
		);
	}

	@Test
	public void testBasicEntityOrderedLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
							.enableOrderedReturn( true )
							.multiLoad( 3, 1 );
					assertThat( results.size(), is( 2 ) );
					// the problem with asserting this is that its in-determinate based on the order we get them back from the database
					assertThat( results.get( 0 ).getId(), is( 3 ) );
					assertThat( results.get( 1 ).getId(), is( 1 ) );
				}
		);
	}

	@Test
	public void testBasicEntityOrderedDeleteCheckLoad(SessionFactoryScope scope) {
		// using ordered results
		scope.inTransaction(
				session -> {
					session.remove( session.get( BasicEntity.class, 2 ) );

					// test control - no delete-checking
					{
						final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
								.enableOrderedReturn( true )
								.enableReturnOfDeletedEntities( true )
								.multiLoad( 3, 2, 1 );
						assertThat( results.size(), is( 3 ) );
						assertThat( results.get( 0 ).getId(), is( 3 ) );
						assertThat( results.get( 1 ).getId(), is( 2 ) );
						assertThat( results.get( 2 ).getId(), is( 1 ) );
					}

					// now apply delete-checking
					{
						final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
								.enableOrderedReturn( true )
								.enableReturnOfDeletedEntities( false )
								.multiLoad( 3, 2, 1 );
						// we should still get 3 results
						assertThat( results.size(), is( 3 ) );
						assertThat( results.get( 0 ).getId(), is( 3 ) );
						// however, the second one should now be null
						assertThat( results.get( 1 ), nullValue() );
						assertThat( results.get( 2 ).getId(), is( 1 ) );
					}
				}
		);

	}

	// todo (6.0) : consider a bunch of collection-based hamcrest Matchers asserting "is initialized", for example:
	//		`assertThat( results, CollectionMatchers.isInitialized() )`
	//
	// todo (6.0) : ^^ another useful one would be a composite matcher checking the elements, e.g.:
	//		````
	//		assertThat(
	//				results,
	//				CollectionMatchers.all( e -> notNullValue( e ) )
	//						.and( e -> isInitialized() )
	//						....
	//		);
	//		````

	@Test
	public void testBasicEntityUnorderedDeleteCheckLoad(SessionFactoryScope scope) {

		// using un-ordered results
		scope.inTransaction(
				session -> {
					session.remove( session.get( BasicEntity.class, 2 ) );

					// test control - no delete-checking
					{
						final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
								.enableReturnOfDeletedEntities( true )
								.multiLoad( 3, 2, 1 );

						assertThat( results.size(), is( 3 ) );

						assertThat(
								results,
								CollectionMatchers.hasNoNullElements()
						);
					}

					// now apply delete-checking
					{
						final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
								.enableReturnOfDeletedEntities( false )
								.multiLoad( 3, 2, 1 );
						// we should still get 3 results
						assertThat( results.size(), is( 3 ) );

						// however, we should now have a null element for the deleted entity
						assertThat(
								results,
								CollectionMatchers.hasNullElements()
						);
					}
				}
		);
	}

	@Test
	public void testMultiLoadingCompositeId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<EntityWithAggregateId> entities = session.byMultipleIds( EntityWithAggregateId.class ).multiLoad(
							new EntityWithAggregateId.Key( "abc", "def" ),
							new EntityWithAggregateId.Key( "123", "456" )
					);

					assertThat( entities.size(), is( 2 ) );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final BasicEntity first = new BasicEntity( 1, "first" );
					final BasicEntity second = new BasicEntity( 2, "second" );
					final BasicEntity third = new BasicEntity( 3, "third" );
					session.persist( first );
					session.persist( second );
					session.persist( third );

					session.persist(
							new EntityWithAggregateId(
									new EntityWithAggregateId.Key( "abc", "def"),
									"ghi"
							)
					);

					session.persist(
							new EntityWithAggregateId(
									new EntityWithAggregateId.Key( "123", "456"),
									"789"
							)
					);
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

}
