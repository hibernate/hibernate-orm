/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.resultmapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.query.named.NamedResultSetMappingMemento;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SimpleEntityWithNamedMappings.class,
				DiscriminatedRoot.class,
				DiscriminatedSubType1.class,
				DiscriminatedSubType2.class
		}
)
public class EntityResultTests extends BaseUsageTest {

	@Test
	public void testSimpleEntityResultMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento( "entity" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity" );
				}
		);
	}

	@Test
	public void testImplicitAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento(
									"entity-none" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-none" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-none" );
				}
		);
	}

	@Test
	public void testMixedAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedQueryRepository()
							.getResultSetMappingMemento(
									"entity-id-notes" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-id-notes" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-id-notes" );
				}
		);
	}

	@Test
	public void testExplicitDiscriminatedMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String qryString =
							"select id as id_alias,"
							+ "   type_code as type_code_alias,"
							+ "   root_name as root_name_alias,"
							+ "   subtype1_name as subtype1_name_alias,"
							+ "   subtype2_name as subtype2_name_alias"
							+ " from discriminated_entity";

					final List<DiscriminatedRoot> results = session.createNativeQuery( qryString, "root-explicit" ).list();
					assertThat( results.size(), is( 4 ) );

					final Set<Integer> idsFound = new HashSet<>();
					results.forEach( result -> idsFound.add( result.getId() ) );
					assertThat( idsFound, containsExpectedValues( 1, 2, 3, 4 ) );
				}
		);
	}

	public <T> Matcher<? extends Collection<T>> containsExpectedValues(T... values) {
		return new BaseMatcher<Collection<T>>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "contain expected values" );
			}

			@Override
			public boolean matches(Object item) {
				if ( ! Collection.class.isInstance( item ) ) {
					return false;
				}

				//noinspection unchecked
				final Collection<Integer> set = (Collection<Integer>) item;
				final boolean containedAll = set.containsAll( Arrays.asList( values ) );

				//noinspection RedundantIfStatement
				if ( ! containedAll  ) {
					return false;
				}

				return true;
			}
		};
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final DiscriminatedRoot root1 = new DiscriminatedRoot( 1, "root-1" );
					final DiscriminatedRoot root2 = new DiscriminatedRoot( 2, "root-2" );
					final DiscriminatedSubType1 subType1_1 = new DiscriminatedSubType1( 3, "root-3", "sub_type1-1" );
					final DiscriminatedSubType2 subType2_1 = new DiscriminatedSubType2( 4, "root-3", "sub_type2-1" );
					session.persist( root1 );
					session.persist( root2 );
					session.persist( subType1_1 );
					session.persist( subType2_1 );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete DiscriminatedRoot" ).executeUpdate()
		);
	}
}
