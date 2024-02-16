/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * (Flaky) test for {@link ConcreteSqmSelectQueryPlan#withCacheableSqmInterpretation} not checking for {@link JdbcOperationQuerySelect#dependsOnParameterBindings()}/{@link JdbcOperationQuerySelect#isCompatibleWith(org.hibernate.sql.exec.spi.JdbcParameterBindings, org.hibernate.query.spi.QueryOptions)} in double-lock checking.
 * 
 * <p>Might cause incorrect SQL to be rendered. In case my MySQL this might cause "limit null,1" statements.
 * 
 * @see https://hibernate.atlassian.net/browse/HHH-17742
 */
@RequiresDialect(MySQLDialect.class)
public class ConcurrentConcreteSqmSelectQueryPlainTest extends BaseCoreFunctionalTestCase {

	public static final String QUERY_STRING = "select e from simple e";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	/**
	 * First query will generated a "limit ?,?" SQL statement, the following ones only need "limit ?". 
	 * Due to the race condition, the following ones reuse the cached "limit ?,?" statement, resulting in "limit null,?" being generated. 
	 */
	@Test
	public void run() throws InterruptedException {
		inTransaction( session -> {
			for ( int i = 0; i < 2; i++ ) {
				SimpleEntity entity = new SimpleEntity();
				entity.setId( i );
				session.persist( entity );
			}
		} );

		CompletableFuture<List<SimpleEntity>>[] results = new CompletableFuture[5];
		ExecutorService executorService = Executors.newFixedThreadPool( results.length );

		for ( int i = 0; i < results.length; i++ ) {
			int index = i;
			results[i] = CompletableFuture.supplyAsync( () -> executeQuery( index ), executorService );
		}
		for ( int i = 0; i < results.length; i++ ) {
			assertThat( results[i].join() ).hasSize( 1 );
		}

		executorService.shutdown();
	}

	private List<SimpleEntity> executeQuery(int index) {
		try (Session session = sessionFactory().openSession()) {
			return executeQuery( session, index );
		}
	}

	private List<SimpleEntity> executeQuery(Session session, int index) {
		Query<SimpleEntity> query = session.createQuery( QUERY_STRING, SimpleEntity.class )
				.setMaxResults( 1 );
		
		if ( index == 0 ) {
			query.setFirstResult( 1 );
		} else {
			try {
				Thread.sleep( 500L ); // sleep to "ensure" all queries use the same SelectQueryPlan instance (QuerySqmImpl#resolveSelectQueryPlan)
			}
			catch (InterruptedException ex) {
				fail( "sleep interrupted: query " + index, ex );
			}
		}
				
		return query.list();
	}
	
	@Override
	protected Configuration constructAndConfigureConfiguration(BootstrapServiceRegistry bootstrapServiceRegistry) {
		Configuration cfg = super.constructAndConfigureConfiguration( bootstrapServiceRegistry );
		cfg.setProperty( AvailableSettings.SEMANTIC_QUERY_TRANSLATOR, DelayingStandardSqmTranslatorFactory.class.getName() );

		return cfg;
	}
	
	@Entity(name = "simple")
	public static class SimpleEntity {

		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
	
	public static class DelayingStandardSqmTranslatorFactory extends StandardSqmTranslatorFactory {
		
		@Override
		public SqmTranslator<SelectStatement> createSelectTranslator(SqmSelectStatement<?> sqmSelectStatement, QueryOptions queryOptions,
				DomainParameterXref domainParameterXref, QueryParameterBindings domainParameterBindings, LoadQueryInfluencers loadQueryInfluencers,
				SqlAstCreationContext creationContext, boolean deduplicateSelectionItems) {
			
			try {
				Thread.sleep( 2000L ); // delay to trigger double-lock checking by concurrent queries
			}
			catch (InterruptedException ex) {
				fail( "sleep interrupted: createSelectTranslator", ex );
			}

			return super.createSelectTranslator( sqmSelectStatement, queryOptions, domainParameterXref, domainParameterBindings, loadQueryInfluencers, creationContext,
					deduplicateSelectionItems );
		}
		
	}
	
}
