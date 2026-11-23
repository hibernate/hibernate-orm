package org.hibernate.orm.test.paging.keybased;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = KeyBasedPagingPlanTest.Employee.class)
@ServiceRegistry(settings = @Setting(name = QuerySettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"))
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-21128")
class KeyBasedPagingPlanTest {
	private static final KeyedPage<Employee> PAGE = Page.first( 10 ).keyedBy( Order.asc( Employee.class, "id" ) );

	@BeforeAll
	static void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Employee manager = null;
			for ( int id = 1; id <= 4; id++ ) {
				final var employee = new Employee();
				employee.id = id;
				employee.manager = manager;
				session.persist( employee );
				manager = employee;
			}
		} );
	}

	@Test
	void parameterAndLiteralInDifferentPositions(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var criteria = cb.createQuery( Employee.class );
			final var employee = criteria.from( Employee.class );
			final var id = employee.<Integer>get( "id" );

			criteria.where( cb.or( cb.notEqual( id, cb.value( 1 ) ), cb.equal( id, cb.literal( 2 ) ) ) );
			assertThat( session.createSelectionQuery( criteria ).getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 2, 3, 4 );

			criteria.where( cb.or( cb.equal( id, cb.literal( 1 ) ).not(), cb.equal( id, cb.value( 2 ) ) ) );
			assertThat( session.createSelectionQuery( criteria ).getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 2, 3, 4 );

			criteria.where( cb.or( cb.equal( id, cb.literal( 1 ) ), cb.equal( id, cb.value( 2 ) ) ) );
			assertThat( session.createSelectionQuery( criteria ).getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 1, 2 );
		} );
	}

	@Test
	void reusePlanWithDifferentBoundValue(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var criteria = cb.createQuery( Employee.class );
			final var employee = criteria.from( Employee.class );
			final var id = employee.<Integer>get( "id" );

			criteria.where( cb.equal( id, cb.value( 1 ) ) );
			assertThat( session.createSelectionQuery( criteria ).getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 1 );

			final var statistics = scope.getSessionFactory().getStatistics();
			final long hits = statistics.getQueryPlanCacheHitCount();
			criteria.where( cb.equal( id, cb.value( 2 ) ) );
			assertThat( session.createSelectionQuery( criteria ).getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 2 );
			assertThat( statistics.getQueryPlanCacheHitCount() ).isGreaterThan( hits );
		} );
	}

	@Test
	void dottedKeyPath(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createSelectionQuery( "from Employee where id > 2", Employee.class );
			final var first = query.getKeyedResultList( Page.first( 1 ).keyedBy( Order.desc( Employee.class, "manager.id" ) ) );
			assertThat( first.getResultList() ).extracting( e -> e.id ).containsExactly( 4 );
			assertThat( first.getKeyList() ).containsExactly( List.of( 3 ) );
			final var second = query.getKeyedResultList( first.getNextPage() );
			assertThat( second.getResultList() ).extracting( e -> e.id ).containsExactly( 3 );
			assertThat( second.getKeyList() ).containsExactly( List.of( 2 ) );
			assertThat( second.isLastPage() ).isTrue();
		} );
	}

	@Test
	void nestedJoinAfterCrossJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String hql = """
					select e from Employee e join e.manager m join m.manager mm
					cross join Employee x join %s.manager owner
					where owner.id = 1 and x.id = 1
					""";
			final var first = session.createSelectionQuery( hql.formatted( "m" ), Employee.class );
			final var second = session.createSelectionQuery( hql.formatted( "mm" ), Employee.class );
			assertThat( first.getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 3 );
			assertThat( second.getKeyedResultList( PAGE ).getResultList() )
					.extracting( e -> e.id ).containsExactly( 4 );
			assertThat( second.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString() )
					.contains( "join mm.manager owner" );
		} );
	}

	@Test
	void disjunctionInWhereClause(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// the key restriction of each later page must not be absorbed by the disjunction
			final var query = session.createSelectionQuery( "from Employee where id = 1 or id >= 3", Employee.class );
			final List<Integer> ids = new ArrayList<>();
			KeyedPage<Employee> page = Page.first( 1 ).keyedBy( Order.asc( Employee.class, "id" ) );
			while ( ids.size() <= 4 ) {
				final var results = query.getKeyedResultList( page );
				results.getResultList().forEach( employee -> ids.add( employee.id ) );
				if ( results.isLastPage() ) {
					break;
				}
				page = results.getNextPage();
			}
			assertThat( ids ).containsExactly( 1, 3, 4 );
		} );
	}

	@Entity(name = "Employee")
	static class Employee {
		@Id
		int id;
		@ManyToOne
		Employee manager;
	}
}
