package org.hibernate.orm.test.query.sql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = NativeQueryParameterCacheTest.CacheRecord.class)
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-20443")
class NativeQueryParameterCacheTest {
	private static final String SELECT = "select id, name from native_parameter_cache where id = :";
	private static final String ADJUSTED_SELECT = "select id, name from native_parameter_cache where id = ?";
	private static final Row FIRST = new Row( 1, "first" );
	private static final Row SECOND = new Row( 2, "second" );
	private static final Row THIRD = new Row( 3, "third" );

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CacheRecord( 1, "first", 1, 2 ) );
			session.persist( new CacheRecord( 2, "second", 2, 1 ) );
			session.persist( new CacheRecord( 3, "third", 3, 4 ) );
		} );
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
		scope.getSessionFactory().getStatistics().clear();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource({ "foo,bar,false", "bar,foo,false", "foo,bar,true", "bar,foo,true" })
	void differentParameterNames(String first, String second, boolean scroll, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( results( session.createNativeQuery( SELECT + first, "row", Row.class )
					.setParameter( first, 1 ), scroll ) ).containsExactly( FIRST );
			assertThat( results( session.createNativeQuery( SELECT + second, "row", Row.class )
					.setParameter( second, 2 ), scroll ) ).containsExactly( SECOND );
			assertThat( results( session.createNativeQuery( SELECT + first, "row", Row.class )
					.setParameter( first, 3 ), scroll ) ).containsExactly( THIRD );
		} );
		assertCachedPlans( scope, 2 );
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "true,false", "false,true", "true,true" })
	void permutedParameterNames(boolean reverse, boolean scroll, SessionFactoryScope scope) {
		String first = reverse ? "bar" : "foo";
		String second = reverse ? "foo" : "bar";
		String prefix = "select id, name from native_parameter_cache where first_value = :";
		scope.inTransaction( session -> {
			assertThat( results( session.createNativeQuery(
					prefix + first + " and second_value = :" + second, "row", Row.class )
					.setParameter( "foo", 1 ).setParameter( "bar", 2 ), scroll ) )
					.containsExactly( reverse ? SECOND : FIRST );
			assertThat( results( session.createNativeQuery(
					prefix + second + " and second_value = :" + first, "row", Row.class )
					.setParameter( "foo", 1 ).setParameter( "bar", 2 ), scroll ) )
					.containsExactly( reverse ? FIRST : SECOND );
		} );
		assertCachedPlans( scope, 2 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void repeatedParameterOccurrences(boolean reverse, SessionFactoryScope scope) {
		String first = reverse ? "bar" : "foo";
		String second = reverse ? "foo" : "bar";
		String prefix = "select id, name from native_parameter_cache where first_value = :";
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery(
					prefix + first + " or second_value = :" + first + " order by id", "row", Row.class )
					.setParameter( first, 1 ).list() ).containsExactly( FIRST, SECOND );
			assertThat( session.createNativeQuery(
					prefix + second + " or second_value = :" + second + " order by id", "row", Row.class )
					.setParameter( second, 3 ).list() ).containsExactly( THIRD );
		} );
		assertCachedPlans( scope, 2 );
	}

	@ParameterizedTest
	@CsvSource({ "foo,bar", "bar,foo" })
	void registeredNamedQueries(String first, String second, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.getSessionFactory().addNamedQuery( "firstNative",
					session.createNativeQuery( SELECT + first, "row", Row.class ) );
			session.getSessionFactory().addNamedQuery( "secondNative",
					session.createNativeQuery( SELECT + second, "row", Row.class ) );
			assertThat( session.createNamedQuery( "firstNative", "row", Row.class )
					.setParameter( first, 1 ).list() ).containsExactly( FIRST );
			assertThat( session.createNamedQuery( "secondNative", "row", Row.class )
					.setParameter( second, 2 ).list() ).containsExactly( SECOND );
		} );
		assertCachedPlans( scope, 2 );
	}

	@Test
	void identicalSqlReusesPlanForListAndScroll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( SELECT + "foo", "row", Row.class )
					.setParameter( "foo", 1 ).list() ).containsExactly( FIRST );
			assertThat( session.createNativeQuery( SELECT + "foo", "row", Row.class )
					.setParameter( "foo", 2 ).list() ).containsExactly( SECOND );
			assertThat( results( session.createNativeQuery( SELECT + "foo", "row", Row.class )
					.setParameter( "foo", 3 ), true ) ).containsExactly( THIRD );
		} );
		assertCachedPlans( scope, 1 );
		var statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.getQueryPlanCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getQueryPlanCacheHitCount() ).isEqualTo( 2 );
	}

	@Test
	void queryStringAndStatisticsUseAdjustedSql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var first = session.createNativeQuery( SELECT + "foo", "row", Row.class );
			assertThat( first.getQueryString() ).isEqualTo( ADJUSTED_SELECT );
			assertThat( first.setParameter( "foo", 1 ).list() ).containsExactly( FIRST );
			assertThat( session.createNativeQuery( SELECT + "foo", "row", Row.class )
					.setParameter( "foo", 2 ).list() ).containsExactly( SECOND );
		} );
		var statistics = scope.getSessionFactory().getStatistics().getQueryStatistics( ADJUSTED_SELECT );
		assertThat( statistics.getExecutionCount() ).isEqualTo( 2 );
		assertThat( statistics.getPlanCacheMissCount() ).isEqualTo( 1 );
		assertThat( statistics.getPlanCacheHitCount() ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void nativeMutationParameterOrder(boolean reverse, SessionFactoryScope scope) {
		String first = reverse ? "bar" : "foo";
		String second = reverse ? "foo" : "bar";
		String prefix = "update native_parameter_cache set first_value = :";
		scope.inTransaction( session -> {
			assertThat( session.createNativeMutationQuery( prefix + first + " where id = :" + second )
					.setParameter( first, 11 ).setParameter( second, 1 ).executeUpdate() ).isEqualTo( 1 );
			assertThat( session.createNativeMutationQuery( prefix + second + " where id = :" + first )
					.setParameter( second, 22 ).setParameter( first, 2 ).executeUpdate() ).isEqualTo( 1 );
			session.clear();
			assertThat( session.find( CacheRecord.class, 1 ).firstValue ).isEqualTo( 11 );
			assertThat( session.find( CacheRecord.class, 2 ).firstValue ).isEqualTo( 22 );
			assertThat( session.find( CacheRecord.class, 3 ).firstValue ).isEqualTo( 3 );
		} );
	}

	private static List<Row> results(Query<Row> query, boolean scroll) {
		if ( !scroll ) {
			return query.list();
		}
		List<Row> rows = new ArrayList<>();
		try ( var results = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
			while ( results.next() ) {
				rows.add( results.get() );
			}
		}
		return rows;
	}

	private static void assertCachedPlans(SessionFactoryScope scope, int count) {
		assertThat( scope.getSessionFactory().getQueryEngine().getInterpretationCache().getNumberOfCachedQueryPlans() )
				.isEqualTo( count );
	}

	public record Row(Integer id, String name) {
	}

	@Entity(name = "CacheRecord")
	@Table(name = "native_parameter_cache")
	@SqlResultSetMapping(name = "row", classes = @ConstructorResult(targetClass = Row.class, columns = {
			@ColumnResult(name = "id", type = Integer.class),
			@ColumnResult(name = "name", type = String.class)
	}))
	public static class CacheRecord {
		@Id
		Integer id;
		String name;
		@Column(name = "first_value")
		Integer firstValue;
		@Column(name = "second_value")
		Integer secondValue;

		public CacheRecord() {
		}

		CacheRecord(Integer id, String name, Integer firstValue, Integer secondValue) {
			this.id = id;
			this.name = name;
			this.firstValue = firstValue;
			this.secondValue = secondValue;
		}
	}
}
