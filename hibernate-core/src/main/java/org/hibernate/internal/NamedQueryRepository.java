/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.procedure.ProcedureCallMemento;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NamedQueryRepository {
	private static final Logger log = Logger.getLogger( NamedQueryRepository.class );

	private final Map<String, ResultSetMappingDefinition> namedSqlResultSetMappingMap;

	private volatile Map<String, NamedQueryDefinition> namedQueryDefinitionMap;
	private volatile Map<String, NamedSQLQueryDefinition> namedSqlQueryDefinitionMap;
	private volatile Map<String, ProcedureCallMemento> procedureCallMementoMap;

	public NamedQueryRepository(
			Iterable<NamedQueryDefinition> namedQueryDefinitions,
			Iterable<NamedSQLQueryDefinition> namedSqlQueryDefinitions,
			Iterable<ResultSetMappingDefinition> namedSqlResultSetMappings,
			Map<String, ProcedureCallMemento> namedProcedureCalls) {
		final HashMap<String, NamedQueryDefinition> namedQueryDefinitionMap = new HashMap<String, NamedQueryDefinition>();
		for ( NamedQueryDefinition namedQueryDefinition : namedQueryDefinitions ) {
			namedQueryDefinitionMap.put( namedQueryDefinition.getName(), namedQueryDefinition );
		}
		this.namedQueryDefinitionMap = Collections.unmodifiableMap( namedQueryDefinitionMap );


		final HashMap<String, NamedSQLQueryDefinition> namedSqlQueryDefinitionMap = new HashMap<String, NamedSQLQueryDefinition>();
		for ( NamedSQLQueryDefinition namedSqlQueryDefinition : namedSqlQueryDefinitions ) {
			namedSqlQueryDefinitionMap.put( namedSqlQueryDefinition.getName(), namedSqlQueryDefinition );
		}
		this.namedSqlQueryDefinitionMap = Collections.unmodifiableMap( namedSqlQueryDefinitionMap );

		final HashMap<String, ResultSetMappingDefinition> namedSqlResultSetMappingMap = new HashMap<String, ResultSetMappingDefinition>();
		for ( ResultSetMappingDefinition resultSetMappingDefinition : namedSqlResultSetMappings ) {
			namedSqlResultSetMappingMap.put( resultSetMappingDefinition.getName(), resultSetMappingDefinition );
		}
		this.namedSqlResultSetMappingMap = Collections.unmodifiableMap( namedSqlResultSetMappingMap );
		this.procedureCallMementoMap = Collections.unmodifiableMap( namedProcedureCalls );
	}


	public NamedQueryDefinition getNamedQueryDefinition(String queryName) {
		return namedQueryDefinitionMap.get( queryName );
	}

	public NamedSQLQueryDefinition getNamedSQLQueryDefinition(String queryName) {
		return namedSqlQueryDefinitionMap.get( queryName );
	}

	public ProcedureCallMemento getNamedProcedureCallMemento(String name) {
		return procedureCallMementoMap.get( name );
	}

	public ResultSetMappingDefinition getResultSetMappingDefinition(String mappingName) {
		return namedSqlResultSetMappingMap.get( mappingName );
	}

	public synchronized void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {
		if ( NamedSQLQueryDefinition.class.isInstance( definition ) ) {
			throw new IllegalArgumentException( "NamedSQLQueryDefinition instance incorrectly passed to registerNamedQueryDefinition" );
		}

		if ( ! name.equals( definition.getName() ) ) {
			definition = definition.makeCopy( name );
		}

		final Map<String, NamedQueryDefinition> copy = CollectionHelper.makeCopy( namedQueryDefinitionMap );
		final NamedQueryDefinition previous = copy.put( name, definition );
		if ( previous != null ) {
			log.debugf(
					"registering named query definition [%s] overriding previously registered definition [%s]",
					name,
					previous
			);
		}

		this.namedQueryDefinitionMap = Collections.unmodifiableMap( copy );
	}

	public synchronized void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {
		if ( ! name.equals( definition.getName() ) ) {
			definition = definition.makeCopy( name );
		}

		final Map<String, NamedSQLQueryDefinition> copy = CollectionHelper.makeCopy( namedSqlQueryDefinitionMap );
		final NamedQueryDefinition previous = copy.put( name, definition );
		if ( previous != null ) {
			log.debugf(
					"registering named SQL query definition [%s] overriding previously registered definition [%s]",
					name,
					previous
			);
		}

		this.namedSqlQueryDefinitionMap = Collections.unmodifiableMap( copy );
	}

	public synchronized void registerNamedProcedureCallMemento(String name, ProcedureCallMemento memento) {
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

	public Map<String,HibernateException> checkNamedQueries(QueryPlanCache queryPlanCache) {
		Map<String,HibernateException> errors = new HashMap<String,HibernateException>();

		// Check named HQL queries
		log.debugf( "Checking %s named HQL queries", namedQueryDefinitionMap.size() );
		for ( NamedQueryDefinition namedQueryDefinition : namedQueryDefinitionMap.values() ) {
			// this will throw an error if there's something wrong.
			try {
				log.debugf( "Checking named query: %s", namedQueryDefinition.getName() );
				//TODO: BUG! this currently fails for named queries for non-POJO entities
				queryPlanCache.getHQLQueryPlan( namedQueryDefinition.getQueryString(), false, Collections.EMPTY_MAP );
			}
			catch ( HibernateException e ) {
				errors.put( namedQueryDefinition.getName(), e );
			}


		}

		// Check native-sql queries
		log.debugf( "Checking %s named SQL queries", namedSqlQueryDefinitionMap.size() );
		for ( NamedSQLQueryDefinition namedSQLQueryDefinition : namedSqlQueryDefinitionMap.values() ) {
			// this will throw an error if there's something wrong.
			try {
				log.debugf( "Checking named SQL query: %s", namedSQLQueryDefinition.getName() );
				// TODO : would be really nice to cache the spec on the query-def so as to not have to re-calc the hash;
				// currently not doable though because of the resultset-ref stuff...
				NativeSQLQuerySpecification spec;
				if ( namedSQLQueryDefinition.getResultSetRef() != null ) {
					ResultSetMappingDefinition definition = getResultSetMappingDefinition( namedSQLQueryDefinition.getResultSetRef() );
					if ( definition == null ) {
						throw new MappingException( "Unable to find resultset-ref definition: " + namedSQLQueryDefinition.getResultSetRef() );
					}
					spec = new NativeSQLQuerySpecification(
							namedSQLQueryDefinition.getQueryString(),
							definition.getQueryReturns(),
							namedSQLQueryDefinition.getQuerySpaces()
					);
				}
				else {
					spec =  new NativeSQLQuerySpecification(
							namedSQLQueryDefinition.getQueryString(),
							namedSQLQueryDefinition.getQueryReturns(),
							namedSQLQueryDefinition.getQuerySpaces()
					);
				}
				queryPlanCache.getNativeSQLQueryPlan( spec );
			}
			catch ( HibernateException e ) {
				errors.put( namedSQLQueryDefinition.getName(), e );
			}
		}

		return errors;
	}
}
