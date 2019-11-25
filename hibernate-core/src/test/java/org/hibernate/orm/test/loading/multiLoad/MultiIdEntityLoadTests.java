/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.Collection;
import java.util.List;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
@SessionFactoryFunctionalTesting
@SuppressWarnings("WeakerAccess")
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
					session.delete( session.load( BasicEntity.class, 2 ) );

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
	public void testBasicEntityUnOrderedDeleteCheckLoad(SessionFactoryScope scope) {

		// using un-ordered results
		scope.inTransaction(
				session -> {
					session.delete( session.load( BasicEntity.class, 2 ) );

					// test control - no delete-checking
					{
						final List<BasicEntity> results = session.byMultipleIds( BasicEntity.class )
								.enableReturnOfDeletedEntities( true )
								.multiLoad( 3, 2, 1 );

						assertThat( results.size(), is( 3 ) );

						assertThat(
								results,
								HasNullElementsMatcher.hasNoNullElements()
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
								HasNullElementsMatcher.hasNullElements()
						);
					}
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
					session.save( first );
					session.save( second );
					session.save( third );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity" ).executeUpdate()
		);
	}

	private static class HasNullElementsMatcher<C extends Collection<?>> extends BaseMatcher<C> {
		public static final HasNullElementsMatcher INSTANCE = new HasNullElementsMatcher( false );
		public static final HasNullElementsMatcher NEGATED_INSTANCE = new HasNullElementsMatcher( true );

		public static <X extends Collection<?>> HasNullElementsMatcher<X> hasNullElements() {
			//noinspection unchecked
			return INSTANCE;
		}

		public static <X extends Collection<?>> HasNullElementsMatcher<X> hasNoNullElements() {
			//noinspection unchecked
			return NEGATED_INSTANCE;
		}

		private final boolean negated;

		public HasNullElementsMatcher(boolean negated) {
			this.negated = negated;
		}

		@Override
		public boolean matches(Object item) {
			assertThat( item, instanceOf( Collection.class ) );

			//noinspection unchecked
			C collection = (C) item;

			if ( negated ) {
				// check no-null-elements - if any is null, this check fails
				collection.forEach( e -> assertThat( e, notNullValue() ) );
				return true;
			}

			boolean foundOne = false;
			for ( Object e : collection ) {
				if ( e == null ) {
					foundOne = true;
					break;
				}
			}
			return foundOne;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "had null elements" );
		}
	}
}
