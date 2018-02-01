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
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.internal.QueryInterpretationsImpl;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.internal.SemanticQueryProducerImpl;
import org.hibernate.query.sqm.produce.spi.SemanticQueryProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	private final QueryInterpretations queryInterpretations;
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
		this.queryInterpretations = new QueryInterpretationsImpl( sessionFactory );
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

	public QueryInterpretations getQueryInterpretations() {
		return queryInterpretations;
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	public EntityDescriptor findEntityByName(String name) {
		final Map<String,String> importMap = getSessionFactory().getTypeConfiguration().getImportMap();
		if ( importMap.containsKey( name ) ) {
			name = importMap.get( name );
		}

		final EntityDescriptor entityPersister = getSessionFactory().getTypeConfiguration().resolveEntityDescriptor( name );
		if ( entityPersister != null ) {
			return entityPersister;
		}

		return null;
	}

	public void close() {

	}
}
