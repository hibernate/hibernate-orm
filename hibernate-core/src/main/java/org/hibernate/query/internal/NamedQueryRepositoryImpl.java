/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.ResultSetMappingDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@Incubating
public class NamedQueryRepositoryImpl implements NamedQueryRepository {
	private static final Logger log = Logger.getLogger( NamedQueryRepository.class );

	private final Map<String, ResultSetMappingDescriptor> namedSqlResultSetMappingMap;

	private volatile Map<String, NamedHqlQueryDescriptor> namedHqlQueryDescriptorMap;
	private volatile Map<String, NamedNativeQueryDescriptor> namedNativeQueryDescriptorMap;
	private volatile Map<String, ProcedureCallMemento> procedureCallMementoMap;

	public NamedQueryRepositoryImpl(
			Iterable<NamedHqlQueryDescriptor> namedHqlQueryDescriptors,
			Iterable<NamedNativeQueryDescriptor> namedNativeQueryDescriptors,
			Iterable<ResultSetMappingDescriptor> namedSqlResultSetMappings,
			Map<String, ProcedureCallMemento> namedProcedureCalls) {
		final HashMap<String, NamedHqlQueryDescriptor> namedQueryDefinitionMap = new HashMap<>();
		for ( NamedHqlQueryDescriptor namedHqlQueryDescriptor : namedHqlQueryDescriptors ) {
			namedQueryDefinitionMap.put( namedHqlQueryDescriptor.getName(), namedHqlQueryDescriptor );
		}
		this.namedHqlQueryDescriptorMap = Collections.unmodifiableMap( namedQueryDefinitionMap );


		final HashMap<String, NamedNativeQueryDescriptor> namedSqlQueryDefinitionMap = new HashMap<>();
		for ( NamedNativeQueryDescriptor namedNativeQueryDescriptor : namedNativeQueryDescriptors ) {
			namedSqlQueryDefinitionMap.put( namedNativeQueryDescriptor.getName(), namedNativeQueryDescriptor );
		}
		this.namedNativeQueryDescriptorMap = Collections.unmodifiableMap( namedSqlQueryDefinitionMap );

		final HashMap<String, ResultSetMappingDescriptor> namedSqlResultSetMappingMap = new HashMap<>();
		for ( ResultSetMappingDescriptor resultSetMappingDefinition : namedSqlResultSetMappings ) {
			namedSqlResultSetMappingMap.put( resultSetMappingDefinition.getName(), resultSetMappingDefinition );
		}
		this.namedSqlResultSetMappingMap = Collections.unmodifiableMap( namedSqlResultSetMappingMap );
		this.procedureCallMementoMap = Collections.unmodifiableMap( namedProcedureCalls );
	}

	public NamedQueryRepositoryImpl(
			Map<String,NamedHqlQueryDescriptor> namedHqlQueryDescriptorMap,
			Map<String,NamedNativeQueryDescriptor> namedNativeQueryDescriptorMap,
			Map<String,ResultSetMappingDescriptor> namedSqlResultSetMappingMap,
			Map<String, ProcedureCallMemento> namedProcedureCallMap) {
		this.namedHqlQueryDescriptorMap = Collections.unmodifiableMap( namedHqlQueryDescriptorMap );
		this.namedNativeQueryDescriptorMap = Collections.unmodifiableMap( namedNativeQueryDescriptorMap );
		this.namedSqlResultSetMappingMap = Collections.unmodifiableMap( namedSqlResultSetMappingMap );
		this.procedureCallMementoMap = Collections.unmodifiableMap( namedProcedureCallMap );
	}

	@Override
	public NamedHqlQueryDescriptor getNamedHqlDescriptor(String queryName) {
		return namedHqlQueryDescriptorMap.get( queryName );
	}

	@Override
	public NamedNativeQueryDescriptor getNamedNativeDescriptor(String queryName) {
		return namedNativeQueryDescriptorMap.get( queryName );
	}

	@Override public ProcedureCallMemento getNamedProcedureCallMemento(String name) {
		return procedureCallMementoMap.get( name );
	}

	@Override public ResultSetMappingDescriptor getResultSetMappingDescriptor(String mappingName) {
		return namedSqlResultSetMappingMap.get( mappingName );
	}

	@Override public synchronized void registerNamedHqlQueryDescriptor(String name, NamedHqlQueryDescriptor descriptor) {
		// todo (6.0) : shouldn't we make the copy anyway?
		if ( ! name.equals( descriptor.getName() ) ) {
			descriptor = descriptor.makeCopy( name );
		}

		final Map<String, NamedHqlQueryDescriptor> copy = CollectionHelper.makeCopy( namedHqlQueryDescriptorMap );
		final NamedHqlQueryDescriptor previous = copy.put( name, descriptor );
		if ( previous != null ) {
			log.debugf(
					"registering named query descriptor [%s] overriding previously registered descriptor [%s]",
					name,
					previous
			);
		}

		this.namedHqlQueryDescriptorMap = Collections.unmodifiableMap( copy );
	}

	@Override
	public void registerNamedNativeQueryDescriptor(
			String name,
			NamedNativeQueryDescriptor descriptor) {
		if ( ! name.equals( descriptor.getName() ) ) {
			descriptor = descriptor.makeCopy( name );
		}

		final Map<String, NamedNativeQueryDescriptor> copy = CollectionHelper.makeCopy( namedNativeQueryDescriptorMap );
		final NamedNativeQueryDescriptor previous = copy.put( name, descriptor );
		if ( previous != null ) {
			log.debugf(
					"registering named SQL query descriptor [%s] overriding previously registered descriptor [%s]",
					name,
					previous
			);
		}

		this.namedNativeQueryDescriptorMap = Collections.unmodifiableMap( copy );
	}

	@Override public synchronized void registerNamedProcedureCallMemento(String name, ProcedureCallMemento memento) {
		final Map<String, ProcedureCallMemento> copy = CollectionHelper.makeCopy( procedureCallMementoMap );
		final ProcedureCallMemento previous = copy.put( name, memento );
		if ( previous != null ) {
			log.debugf(
					"registering named procedure call definition [%s] overriding previously registered definition [%s]",
					name,
					previous
			);
		}

		this.procedureCallMementoMap = Collections.unmodifiableMap( copy );
	}

	@Override
	public Map<String,HibernateException> checkNamedQueries(QueryEngine queryEngine) {
		Map<String,HibernateException> errors = new HashMap<>();

		// Check named HQL queries
		log.debugf( "Checking %s named HQL queries", namedHqlQueryDescriptorMap.size() );
		for ( NamedHqlQueryDescriptor namedHqlQueryDescriptor : namedHqlQueryDescriptorMap.values() ) {
			//		1) build QueryOptions reference from `namedHqlQueryDescriptor`
			//		2) build `ParsingContext` from the QueryEngine's SessionFactory
			//		3) need to resolve the `SelectQueryPlan` - roughly the logic in `NativeQueryImpl#resolveSelectQueryPlan`
			//		4)
			// this will throw an error if there's something wrong.
			try {
				log.debugf( "Checking named query: %s", namedHqlQueryDescriptor.getName() );
				// todo (6.0) : this just builds the SQM AST - should it also try to build the SQL AST?
				//		relatedly - do we want to cache these generated plans?
				queryEngine.getSemanticQueryProducer().interpret( namedHqlQueryDescriptor.getQueryString() );
			}
			catch ( HibernateException e ) {
				errors.put( namedHqlQueryDescriptor.getName(), e );
			}
		}

		// Check native-sql queries
		log.debugf( "Checking %s named SQL queries", namedNativeQueryDescriptorMap.size() );
		for ( NamedNativeQueryDescriptor namedNativeQueryDescriptor : namedNativeQueryDescriptorMap.values() ) {
			// this will throw an error if there's something wrong.
			try {
				log.debugf( "Checking named SQL query: %s", namedNativeQueryDescriptor.getName() );

				// todo (6.0) : NativeQuery still needs some work.
				//		1) consider this use case
				//		2) consider ways to better allow OGM to redefine native query handling.  One
				//			option here is to redefine NativeQueryInterpreter to handle:
				//				a) parameter recognition - it does this already
				//				b) build a org.hibernate.query.spi.SelectQueryPlan (what inputs?) - this
				//					would mean re-purposing
				//
				// so for now, just throw an exception

				throw new NotYetImplementedFor6Exception(  );
//
//				// resolve the named ResultSetMappingDefinition, if one
//				final ResultSetMappingDefinition resultset = namedNativeQueryDescriptor.getResultSetRef() == null
//						? null
//						: getResultSetMappingDescriptor( namedNativeQueryDescriptor.getResultSetRef() );
//
//				// perform parameter recognition
//				final ParameterRecognizerImpl parameterRecognizer = new ParameterRecognizerImpl( queryEngine.getSessionFactory() );
//				queryEngine.getSessionFactory().getServiceRegistry()
//						.getService( NativeQueryInterpreter.class )
//						.recognizeParameters( namedNativeQueryDescriptor.getQuery(), parameterRecognizer );
//				parameterRecognizer.validate();
//
//				// prepare the NativeSelectQueryDefinition
//				final NativeSelectQueryDefinition selectQueryDefinition = new NativeSelectQueryDefinition() {
//					@Override
//					public String getSqlString() {
//						return namedNativeQueryDescriptor.getQuery();
//					}
//
//					@Override
//					public boolean isCallable() {
//						return namedNativeQueryDescriptor.isCallable();
//					}
//
//					@Override
//					public List<JdbcParameterBinder> getParameterBinders() {
//						return parameterRecognizer.getParameterBinders();
//					}
//
//					@Override
//					public ResultSetMappingDescriptor getResultSetMapping() {
//						resultset.
//						return resultset;
//					}
//
//					@Override
//					public RowTransformer getRowTransformer() {
//						return null;
//					}
//				};
//
////				// TODO : would be really nice to cache the spec on the query-def so as to not have to re-calc the hash;
////				// currently not doable though because of the ResultSet-ref stuff...
////				NativeSQLQuerySpecification spec;
////				if ( namedNativeQueryDescriptor.getResultSetRef() != null ) {
////					ResultSetMappingDefinition definition = getResultSetMappingDescriptor( namedNativeQueryDescriptor.getResultSetRef() );
////					if ( definition == null ) {
////						throw new MappingException( "Unable to find resultset-ref definition: " + namedNativeQueryDescriptor.getResultSetRef() );
////					}
////					spec = new NativeSQLQuerySpecification(
////							namedNativeQueryDescriptor.getQueryString(),
////							definition.getResultBuilders(),
////							namedNativeQueryDescriptor.getQuerySpaces()
////					);
////				}
////				else {
////					spec =  new NativeSQLQuerySpecification(
////							namedNativeQueryDescriptor.getQueryString(),
////							namedNativeQueryDescriptor.getQueryResultBuilders(),
////							namedNativeQueryDescriptor.getQuerySpaces()
////					);
////				}
////				queryEngine.getNativeSQLQueryPlan( spec );
			}
			catch ( HibernateException e ) {
				errors.put( namedNativeQueryDescriptor.getName(), e );
			}
		}

		return errors;
	}
}
