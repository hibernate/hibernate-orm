package org.hibernate.orm.test.cache;

import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		QueryCacheWithFilterTest.Person.class,
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public class QueryCacheWithFilterTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "John" );
					session.persist( p );
				}
		);
	}

	@Test
	public void testQueryKeyIsImmutable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.enableFilter( "personName" )
							.setParameter( "name", "John" );
					Query<Person> query = session.createQuery(
							"from Person p",
							Person.class
					);
					query.setCacheable( true );

					List<Person> resultList = query.getResultList();
					assertThat( resultList ).hasSize( 1 );

					//cache query should not be affected by the additional filter
					session.enableFilter( "personName2" )
							.setParameter( "name", "John" );
				}
		);
		scope.inTransaction(
				session -> {
					session.enableFilter( "personName" )
							.setParameter( "name", "John" );
					Query<Person> query = session.createQuery(
							"from Person p",
							Person.class
					);
					query.setCacheable( true );

					List<Person> resultList = query.getResultList();
					assertThat( resultList ).hasSize( 1 );

					assertThat( getQueryCacheRegionStatistics( session ).getHitCount() ).isEqualTo( 1 );
				}
		);
	}

	private static CacheRegionStatistics getQueryCacheRegionStatistics(SessionImplementor session) {
		StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
		return statistics.getQueryRegionStatistics( RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME );
	}


	@Entity(name = "Person")
	@FilterDef(
			name = "personName",
			parameters = @ParamDef(
					name = "name",
					type = String.class
			)
	)
	@Filter(
			name = "personName",
			condition = "name = :name"
	)
	@FilterDef(
			name = "personName2",
			parameters = @ParamDef(
					name = "name",
					type = String.class
			)
	)
	@Filter(
			name = "personName2",
			condition = "name = :name"
	)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
