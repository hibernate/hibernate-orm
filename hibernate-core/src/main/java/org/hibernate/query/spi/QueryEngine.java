/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.internal.QueryPlanCacheImpl;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.internal.SemanticQueryProducerImpl;
import org.hibernate.query.sqm.produce.spi.SemanticQueryProducer;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
@Incubating
public class QueryEngine {
	private final SessionFactoryImplementor sessionFactory;

	private final NamedQueryRepository namedQueryRepository;
	private final HibernateCriteriaBuilder criteriaBuilder;
	private final SemanticQueryProducer semanticQueryProducer;
	private final QueryPlanCache queryPlanCache;
	private final SqmFunctionRegistry sqmFunctionRegistry;

	public QueryEngine(
			SessionFactoryImplementor sessionFactory,
			NamedQueryRepository namedQueryRepository,
			SqmFunctionRegistry sqmFunctionRegistry) {
		this.sessionFactory = sessionFactory;
		this.namedQueryRepository = namedQueryRepository;
		this.semanticQueryProducer = new SemanticQueryProducerImpl( sessionFactory );
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo (6.0) : re-enable this, to initialize whatever class ultimately represents our SPI view of the JPA CriteriaBuilder...
		//this.criteriaBuilder = new CriteriaBuilderImpl( sessionFactory );
		this.criteriaBuilder = null;
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.queryPlanCache = new QueryPlanCacheImpl( sessionFactory );
		this.sqmFunctionRegistry = sqmFunctionRegistry;

		//checking for named queries
		if ( sessionFactory.getSessionFactoryOptions().isNamedQueryStartupCheckingEnabled() ) {
			final Map<String, HibernateException> errors = namedQueryRepository.checkNamedQueries( this );
			if ( !errors.isEmpty() ) {
				StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
				String sep = "";
				for ( Map.Entry<String, HibernateException> entry : errors.entrySet() ) {
					QueryMessageLogger.QUERY_LOGGER.namedQueryError( entry.getKey(), entry.getValue() );
					failingQueries.append( sep ).append( entry.getKey() );
					sep = ", ";
				}
				throw new HibernateException( failingQueries.toString() );
			}
		}
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public NamedQueryRepository getNamedQueryRepository() {
		return namedQueryRepository;
	}

	public HibernateCriteriaBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	public SemanticQueryProducer getSemanticQueryProducer() {
		return semanticQueryProducer;
	}

	public QueryPlanCache getQueryPlanCache() {
		return queryPlanCache;
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	public void close() {
		if ( namedQueryRepository != null ) {
			namedQueryRepository.close();
		}

		if ( criteriaBuilder != null ) {
			criteriaBuilder.close();
		}

		if ( semanticQueryProducer != null ) {
			semanticQueryProducer.close();
		}

		if ( queryPlanCache != null ) {
			queryPlanCache.close();
		}

		if ( sqmFunctionRegistry != null ) {
			sqmFunctionRegistry.close();
		}
	}
}
