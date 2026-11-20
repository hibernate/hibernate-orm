package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same aggregate function of the elements or indices of two collections
 * of one entity, in one query, is evaluated for each collection.
 */
@DomainModel(annotatedClasses = AggregatesOfTwoCollectionsTest.Owner.class)
@SessionFactory
@JiraKey("HHH-21215")
public class AggregatesOfTwoCollectionsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner owner = new Owner( 1 );
			owner.names.addAll( List.of( "a", "z" ) );
			owner.plainNames.addAll( List.of( "a", "z" ) );
			owner.numbers.addAll( List.of( 1, 2 ) );
			owner.otherNumbers.addAll( List.of( 10, 20, 30 ) );
			owner.shortList.addAll( List.of( "x", "y" ) );
			owner.longList.addAll( List.of( "x", "y", "z" ) );
			session.persist( owner );
		} );
		// soft-deletes the greatest of the names, leaving the plain names alone
		scope.inTransaction( session -> session.find( Owner.class, 1 ).names.remove( "z" ) );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void restrictedAndPlainCollection(SessionFactoryScope scope) {
		assertRow( scope, "select max(elements(o.names)), max(elements(o.plainNames)) from Owner o", "a", "z" );
		assertRow( scope, "select max(elements(o.plainNames)), max(elements(o.names)) from Owner o", "z", "a" );
		assertRow( scope, "select min(elements(o.names)), min(elements(o.plainNames)) from Owner o", "a", "a" );
	}

	@Test
	public void restrictedAndPlainCollectionInWhereClause(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( List.of( 1 ), session.createSelectionQuery(
					"select o.id from Owner o where max(elements(o.names)) = 'a' and max(elements(o.plainNames)) = 'z'",
					Integer.class ).getResultList() );
			assertEquals( List.of(), session.createSelectionQuery(
					"select o.id from Owner o where max(elements(o.names)) = max(elements(o.plainNames))",
					Integer.class ).getResultList() );
		} );
	}

	@ParameterizedTest
	@CsvSource({"max,2,30", "min,1,10", "sum,3,60", "avg,1.5,20"})
	public void elements(String function, double first, double second, SessionFactoryScope scope) {
		assertNumbers( scope,
				"select " + function + "(elements(o.numbers)), " + function + "(elements(o.otherNumbers)) from Owner o",
				first, second );
		assertNumbers( scope,
				"select " + function + "(elements(o.otherNumbers)), " + function + "(elements(o.numbers)) from Owner o",
				second, first );
	}

	@ParameterizedTest
	@CsvSource({"max,1,2", "sum,1,3", "avg,0.5,1"})
	public void indices(String function, double first, double second, SessionFactoryScope scope) {
		assertNumbers( scope,
				"select " + function + "(indices(o.shortList)), " + function + "(indices(o.longList)) from Owner o",
				first, second );
		assertNumbers( scope,
				"select " + function + "(indices(o.longList)), " + function + "(indices(o.shortList)) from Owner o",
				second, first );
	}

	@Test
	public void elementAndIndexOfOneCollection(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Object[] row = session.createSelectionQuery(
					"select max(indices(o.longList)), max(elements(o.longList)), max(elements(o.longList)) from Owner o",
					Object[].class ).getSingleResult();
			assertThat( ( (Number) row[0] ).intValue() ).isEqualTo( 2 );
			assertThat( row[1] ).isEqualTo( "z" );
			assertThat( row[2] ).isEqualTo( "z" );
		} );
	}

	private static void assertRow(SessionFactoryScope scope, String hql, Object first, Object second) {
		scope.inSession( session -> {
			final Object[] row = session.createSelectionQuery( hql, Object[].class ).getSingleResult();
			assertThat( row ).containsExactly( first, second );
		} );
	}

	private static void assertNumbers(SessionFactoryScope scope, String hql, double first, double second) {
		scope.inSession( session -> {
			final Object[] row = session.createSelectionQuery( hql, Object[].class ).getSingleResult();
			assertThat( ( (Number) row[0] ).doubleValue() ).isEqualTo( first );
			assertThat( ( (Number) row[1] ).doubleValue() ).isEqualTo( second );
		} );
	}

	@Entity(name = "Owner")
	@Table(name = "two_coll_owner")
	public static class Owner {
		@Id
		Integer id;

		@ElementCollection
		@CollectionTable(name = "two_coll_names")
		@SoftDelete
		Set<String> names = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "two_coll_plain_names")
		Set<String> plainNames = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "two_coll_numbers")
		Set<Integer> numbers = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "two_coll_other_numbers")
		Set<Integer> otherNumbers = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "two_coll_short_list")
		@OrderColumn(name = "pos")
		List<String> shortList = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "two_coll_long_list")
		@OrderColumn(name = "pos")
		List<String> longList = new ArrayList<>();

		public Owner() {
		}

		public Owner(Integer id) {
			this.id = id;
		}
	}
}
