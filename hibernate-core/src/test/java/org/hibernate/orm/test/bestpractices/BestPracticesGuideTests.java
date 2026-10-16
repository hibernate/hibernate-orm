package org.hibernate.orm.test.bestpractices;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises the Best Practices measurement and logging examples without timing assertions.
///
/// @author Steve Ebersole
@BaseUnitTest
class BestPracticesGuideTests {

	@Test
	void compareEquivalentWorkWithDifferentFetchPlans() {
		var configuration = new HibernatePersistenceConfiguration( "best-practices-guide" )
				.managedClasses( Book.class, Author.class )
				.properties( ServiceRegistryUtil.createBaseSettingsWithExport() )
				.property( "hibernate.generate_statistics", true )
				.property( "hibernate.cache.use_second_level_cache", false )
				.property( "hibernate.cache.use_query_cache", false )
				.property( "hibernate.default_batch_fetch_size", 0 );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> {
				for ( long i = 1; i <= 3; i++ ) {
					var author = new Author();
					author.id = i;
					author.name = "Author " + i;
					manager.persist( author );
					var book = new Book();
					book.id = i;
					book.title = "Book " + i;
					book.author = author;
					manager.persist( book );
				}
			} );

			// tag::best-practices-measurement[]
			// tag::best-practices-baseline[]
			var statistics = factory.getStatistics();
			statistics.clear(); // Exclude setup; this factory is private to the test.
			var baseline = factory.callInTransaction( manager ->
					manager.createQuery( "from BestPracticeBook b order by b.id", Book.class )
							.getResultList().stream()
							.map( book -> book.title + ": " + book.author.getName() )
							.toList() );
			long baselinePreparations = statistics.getPrepareStatementCount();
			// end::best-practices-baseline[]

			// tag::best-practices-fetch-plan[]
			statistics.clear();
			var withFetchPlan = factory.callInTransaction( manager ->
					manager.createQuery(
							"from BestPracticeBook b join fetch b.author order by b.id", Book.class )
							.getResultList().stream()
							.map( book -> book.title + ": " + book.author.getName() )
							.toList() );
			long fetchPlanPreparations = statistics.getPrepareStatementCount();
			// end::best-practices-fetch-plan[]

			// tag::best-practices-verification[]
			assertThat( withFetchPlan ).isEqualTo( baseline );
			assertThat( baselinePreparations ).isEqualTo( 4 ); // Books, then three distinct authors.
			assertThat( fetchPlanPreparations ).isEqualTo( 1 );
			assertThat( baseline ).containsExactly(
					"Book 1: Author 1", "Book 2: Author 2", "Book 3: Author 3" );
			// end::best-practices-verification[]
			// end::best-practices-measurement[]
		}
	}

	@Test
	void loggingConfigurationUsesCurrentCategoriesInAnIsolatedContext() throws Exception {
		var globalContext = LogManager.getContext( false );
		try ( var input = getClass().getResourceAsStream( "/best-practices/log4j2.xml" );
				var context = new LoggerContext( "best-practices-example" ) ) {
			var configuration = new XmlConfiguration( context, new ConfigurationSource( input ) );
			context.start( configuration );
			assertThat( configuration.getLoggers() ).containsOnlyKeys(
					"", "org.hibernate.SQL", "org.hibernate.orm.jdbc.bind", "org.hibernate.orm.jdbc.extract" );
			assertThat( context.getLogger( "org.hibernate.SQL" ).getLevel() ).isEqualTo( Level.DEBUG );
			assertThat( context.getLogger( "org.hibernate.orm.jdbc.bind" ).getLevel() ).isEqualTo( Level.TRACE );
			assertThat( context.getLogger( "org.hibernate.orm.jdbc.extract" ).getLevel() ).isEqualTo( Level.TRACE );
			for ( var logger : configuration.getLoggers().values() ) {
				if ( !logger.getName().isEmpty() ) {
					assertThat( logger.isAdditive() ).isFalse();
				}
				assertThat( logger.getAppenders() ).containsOnlyKeys( "CONSOLE" );
				assertThat( logger.getAppenders().get( "CONSOLE" ).isStarted() ).isTrue();
			}
			assertThat( configuration.getRootLogger().getLevel() ).isEqualTo( Level.INFO );
			assertThat( configuration.getRootLogger().getAppenders() ).containsOnlyKeys( "CONSOLE" );
		}
		assertThat( LogManager.getContext( false ) ).isSameAs( globalContext );
	}

	@Entity(name = "BestPracticeBook")
	public static class Book {
		@Id
		Long id;
		String title;
		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		Author author;
	}

	@Entity(name = "BestPracticeAuthor")
	public static class Author {
		@Id
		Long id;
		String name;

		public String getName() {
			return name;
		}
	}
}
