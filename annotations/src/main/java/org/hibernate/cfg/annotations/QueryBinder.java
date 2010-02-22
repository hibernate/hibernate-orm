/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg.annotations;

import java.util.HashMap;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.query.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryRootReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query binder
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
	private static final Logger log = LoggerFactory.getLogger( QueryBinder.class );

	public static void bindQuery(NamedQuery queryAnn, ExtendedMappings mappings, boolean isDefault) {
		if ( queryAnn == null ) return;
		if ( BinderHelper.isDefault( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		//EJBQL Query
		QueryHint[] hints = queryAnn.hints();
		String queryName = queryAnn.query();
		NamedQueryDefinition query = new NamedQueryDefinition(
				queryName,
				getBoolean( queryName, "org.hibernate.cacheable", hints ),
				getString( queryName, "org.hibernate.cacheRegion", hints ),
				getTimeout( queryName, hints ),
				getInteger( queryName, "org.hibernate.fetchSize", hints ),
				getFlushMode( queryName, hints ),
				getCacheMode( queryName, hints ),
				getBoolean( queryName, "org.hibernate.readOnly", hints ),
				getString( queryName, "org.hibernate.comment", hints ),
				null
		);
		if ( isDefault ) {
			mappings.addDefaultQuery( queryAnn.name(), query );
		}
		else {
			mappings.addQuery( queryAnn.name(), query );
		}
		log.info( "Binding Named query: {} => {}", queryAnn.name(), queryAnn.query() );
	}


	public static void bindNativeQuery(NamedNativeQuery queryAnn, ExtendedMappings mappings, boolean isDefault) {
		if ( queryAnn == null ) return;
		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isDefault( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		NamedSQLQueryDefinition query;
		String resultSetMapping = queryAnn.resultSetMapping();
		QueryHint[] hints = queryAnn.hints();
		String queryName = queryAnn.query();
		if ( !BinderHelper.isDefault( resultSetMapping ) ) {
			//sql result set usage
			query = new NamedSQLQueryDefinition(
					queryName,
					resultSetMapping,
					null,
					getBoolean( queryName, "org.hibernate.cacheable", hints ),
					getString( queryName, "org.hibernate.cacheRegion", hints ),
					getTimeout( queryName, hints ),
					getInteger( queryName, "org.hibernate.fetchSize", hints ),
					getFlushMode( queryName, hints ),
					getCacheMode( queryName, hints ),
					getBoolean( queryName, "org.hibernate.readOnly", hints ),
					getString( queryName, "org.hibernate.comment", hints ),
					null,
					getBoolean( queryName, "org.hibernate.callable", hints )
			);
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			//class mapping usage
			//FIXME should be done in a second pass due to entity name?
			final NativeSQLQueryRootReturn entityQueryReturn =
					new NativeSQLQueryRootReturn( "alias1", queryAnn.resultClass().getName(), new HashMap(), LockMode.READ );
			query = new NamedSQLQueryDefinition(
					queryName,
					new NativeSQLQueryReturn[] { entityQueryReturn },
					null,
					getBoolean( queryName, "org.hibernate.cacheable", hints ),
					getString( queryName, "org.hibernate.cacheRegion", hints ),
					getTimeout( queryName, hints ),
					getInteger( queryName, "org.hibernate.fetchSize", hints ),
					getFlushMode( queryName, hints ),
					getCacheMode( queryName, hints ),
					getBoolean( queryName, "org.hibernate.readOnly", hints ),
					getString( queryName, "org.hibernate.comment", hints ),
					null,
					getBoolean( queryName, "org.hibernate.callable", hints )
			);
		}
		else {
			throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
		}
		if ( isDefault ) {
			mappings.addDefaultSQLQuery( queryAnn.name(), query );
		}
		else {
			mappings.addSQLQuery( queryAnn.name(), query );
		}
		log.info( "Binding named native query: {} => {}", queryAnn.name(), queryAnn.query() );
	}

	public static void bindNativeQuery(org.hibernate.annotations.NamedNativeQuery queryAnn, ExtendedMappings mappings) {
		if ( queryAnn == null ) return;
		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isDefault( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		NamedSQLQueryDefinition query;
		String resultSetMapping = queryAnn.resultSetMapping();
		if ( !BinderHelper.isDefault( resultSetMapping ) ) {
			//sql result set usage
			query = new NamedSQLQueryDefinition(
					queryAnn.query(),
					resultSetMapping,
					null,
					queryAnn.cacheable(),
					BinderHelper.isDefault( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
					queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
					queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
					getFlushMode( queryAnn.flushMode() ),
					getCacheMode( queryAnn.cacheMode() ),
					queryAnn.readOnly(),
					BinderHelper.isDefault( queryAnn.comment() ) ? null : queryAnn.comment(),
					null,
					queryAnn.callable()
			);
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			//class mapping usage
			//FIXME should be done in a second pass due to entity name?
			final NativeSQLQueryRootReturn entityQueryReturn =
					new NativeSQLQueryRootReturn( "alias1", queryAnn.resultClass().getName(), new HashMap(), LockMode.READ );
			query = new NamedSQLQueryDefinition(
					queryAnn.query(),
					new NativeSQLQueryReturn[] { entityQueryReturn },
					null,
					queryAnn.cacheable(),
					BinderHelper.isDefault( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
					queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
					queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
					getFlushMode( queryAnn.flushMode() ),
					getCacheMode( queryAnn.cacheMode() ),
					queryAnn.readOnly(),
					BinderHelper.isDefault( queryAnn.comment() ) ? null : queryAnn.comment(),
					null,
					queryAnn.callable()
			);
		}
		else {
			throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
		}
		mappings.addSQLQuery( queryAnn.name(), query );
		log.info( "Binding named native query: {} => {}", queryAnn.name(), queryAnn.query() );
	}

	public static void bindQueries(NamedQueries queriesAnn, ExtendedMappings mappings, boolean isDefault) {
		if ( queriesAnn == null ) return;
		for (NamedQuery q : queriesAnn.value()) {
			bindQuery( q, mappings, isDefault );
		}
	}

	public static void bindNativeQueries(NamedNativeQueries queriesAnn, ExtendedMappings mappings, boolean isDefault) {
		if ( queriesAnn == null ) return;
		for (NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, mappings, isDefault );
		}
	}

	public static void bindNativeQueries(
			org.hibernate.annotations.NamedNativeQueries queriesAnn, ExtendedMappings mappings
	) {
		if ( queriesAnn == null ) return;
		for (org.hibernate.annotations.NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, mappings );
		}
	}

	public static void bindQuery(org.hibernate.annotations.NamedQuery queryAnn, ExtendedMappings mappings) {
		if ( queryAnn == null ) return;
		if ( BinderHelper.isDefault( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		FlushMode flushMode;
		flushMode = getFlushMode( queryAnn.flushMode() );

		NamedQueryDefinition query = new NamedQueryDefinition(
				queryAnn.query(),
				queryAnn.cacheable(),
				BinderHelper.isDefault( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
				queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
				queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
				flushMode,
				getCacheMode( queryAnn.cacheMode() ),
				queryAnn.readOnly(),
				BinderHelper.isDefault( queryAnn.comment() ) ? null : queryAnn.comment(),
				null
		);

		mappings.addQuery( queryAnn.name(), query );
		if ( log.isInfoEnabled() ) log.info( "Binding named query: " + queryAnn.name() + " => " + queryAnn.query() );
	}

	private static FlushMode getFlushMode(FlushModeType flushModeType) {
		FlushMode flushMode;
		switch ( flushModeType ) {
			case ALWAYS:
				flushMode = FlushMode.ALWAYS;
				break;
			case AUTO:
				flushMode = FlushMode.AUTO;
				break;
			case COMMIT:
				flushMode = FlushMode.COMMIT;
				break;
			case NEVER:
				flushMode = FlushMode.MANUAL;
				break;
			case MANUAL:
				flushMode = FlushMode.MANUAL;
				break;
			case PERSISTENCE_CONTEXT:
				flushMode = null;
				break;
			default:
				throw new AssertionFailure( "Unknown flushModeType: " + flushModeType );
		}
		return flushMode;
	}

	private static CacheMode getCacheMode(CacheModeType cacheModeType) {
		switch ( cacheModeType ) {
			case GET:
				return CacheMode.GET;
			case IGNORE:
				return CacheMode.IGNORE;
			case NORMAL:
				return CacheMode.NORMAL;
			case PUT:
				return CacheMode.PUT;
			case REFRESH:
				return CacheMode.REFRESH;
			default:
				throw new AssertionFailure( "Unknown cacheModeType: " + cacheModeType );
		}
	}


	public static void bindQueries(org.hibernate.annotations.NamedQueries queriesAnn, ExtendedMappings mappings) {
		if ( queriesAnn == null ) return;
		for (org.hibernate.annotations.NamedQuery q : queriesAnn.value()) {
			bindQuery( q, mappings );
		}
	}

	public static void bindSqlResultsetMappings(SqlResultSetMappings ann, ExtendedMappings mappings, boolean isDefault) {
		if ( ann == null ) return;
		for (SqlResultSetMapping rs : ann.value()) {
			//no need to handle inSecondPass
			mappings.addSecondPass( new ResultsetMappingSecondPass( rs, mappings, true ) );
		}
	}

	public static void bindSqlResultsetMapping(SqlResultSetMapping ann, ExtendedMappings mappings, boolean isDefault) {
		//no need to handle inSecondPass
		mappings.addSecondPass( new ResultsetMappingSecondPass( ann, mappings, isDefault ) );
	}

	private static CacheMode getCacheMode(String query, QueryHint[] hints) {
		for (QueryHint hint : hints) {
			if ( "org.hibernate.cacheMode".equals( hint.name() ) ) {
				if ( hint.value().equalsIgnoreCase( CacheMode.GET.toString() ) ) {
					return CacheMode.GET;
				}
				else if ( hint.value().equalsIgnoreCase( CacheMode.IGNORE.toString() ) ) {
					return CacheMode.IGNORE;
				}
				else if ( hint.value().equalsIgnoreCase( CacheMode.NORMAL.toString() ) ) {
					return CacheMode.NORMAL;
				}
				else if ( hint.value().equalsIgnoreCase( CacheMode.PUT.toString() ) ) {
					return CacheMode.PUT;
				}
				else if ( hint.value().equalsIgnoreCase( CacheMode.REFRESH.toString() ) ) {
					return CacheMode.REFRESH;
				}
				else {
					throw new AnnotationException( "Unknown CacheMode in hint: " + query + ":" + hint.name() );
				}
			}
		}
		return null;
	}

	private static FlushMode getFlushMode(String query, QueryHint[] hints) {
		for (QueryHint hint : hints) {
			if ( "org.hibernate.flushMode".equals( hint.name() ) ) {
				if ( hint.value().equalsIgnoreCase( FlushMode.ALWAYS.toString() ) ) {
					return FlushMode.ALWAYS;
				}
				else if ( hint.value().equalsIgnoreCase( FlushMode.AUTO.toString() ) ) {
					return FlushMode.AUTO;
				}
				else if ( hint.value().equalsIgnoreCase( FlushMode.COMMIT.toString() ) ) {
					return FlushMode.COMMIT;
				}
				else if ( hint.value().equalsIgnoreCase( FlushMode.NEVER.toString() ) ) {
					return FlushMode.MANUAL;
				}
				else if ( hint.value().equalsIgnoreCase( FlushMode.MANUAL.toString() ) ) {
					return FlushMode.MANUAL;
				}
				else {
					throw new AnnotationException( "Unknown FlushMode in hint: " + query + ":" + hint.name() );
				}
			}
		}
		return null;
	}

	private static boolean getBoolean(String query, String hintName, QueryHint[] hints) {
		for (QueryHint hint : hints) {
			if ( hintName.equals( hint.name() ) ) {
				if ( hint.value().equalsIgnoreCase( "true" ) ) {
					return true;
				}
				else if ( hint.value().equalsIgnoreCase( "false" ) ) {
					return false;
				}
				else {
					throw new AnnotationException( "Not a boolean in hint: " + query + ":" + hint.name() );
				}
			}
		}
		return false;
	}

	private static String getString(String query, String hintName, QueryHint[] hints) {
		for (QueryHint hint : hints) {
			if ( hintName.equals( hint.name() ) ) {
				return hint.value();
			}
		}
		return null;
	}

	private static Integer getInteger(String query, String hintName, QueryHint[] hints) {
		for (QueryHint hint : hints) {
			if ( hintName.equals( hint.name() ) ) {
				try {
					return Integer.decode( hint.value() );
				}
				catch (NumberFormatException nfe) {
					throw new AnnotationException( "Not an integer in hint: " + query + ":" + hint.name(), nfe );
				}
			}
		}
		return null;
	}

	private static Integer getTimeout(String queryName, QueryHint[] hints) {
		Integer timeout = getInteger( queryName, "javax.persistence.query.timeout", hints );

		if ( timeout != null ) {
			// convert milliseconds to seconds
			timeout = new Integer ((int)Math.round(timeout.doubleValue() / 1000.0 ) );
		}
		else {
			// timeout is already in seconds
			timeout = getInteger( queryName, "org.hibernate.timeout", hints );
		}
		return timeout;
	}
}
