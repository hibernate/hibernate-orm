/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.internal.QueryInterpretationsImpl;
import org.hibernate.query.sqm.produce.internal.SemanticQueryProducerImpl;
import org.hibernate.query.sqm.produce.spi.SemanticQueryProducer;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
public class QueryEngine {
	private final SessionFactoryImplementor sessionFactory;

	private final NamedQueryRepository namedQueryRepository;
	private final CriteriaBuilderImpl criteriaBuilder;
	private final SemanticQueryProducer semanticQueryProducer;
	private final QueryInterpretations queryInterpretations;

	public QueryEngine(SessionFactoryImplementor sessionFactory, MetadataImplementor bootTimeModel) {
		this.sessionFactory = sessionFactory;

		this.namedQueryRepository = bootTimeModel.buildNamedQueryRepository( sessionFactory );
		this.semanticQueryProducer = new SemanticQueryProducerImpl( sessionFactory );
		this.criteriaBuilder = new CriteriaBuilderImpl( sessionFactory );
		this.queryInterpretations = new QueryInterpretationsImpl( sessionFactory );

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

	public QueryInterpretations getQueryInterpretations() {
		return queryInterpretations;
	}
}
