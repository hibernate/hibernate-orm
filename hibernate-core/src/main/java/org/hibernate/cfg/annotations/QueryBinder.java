/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.annotations;
import java.util.HashMap;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;

import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Query binder
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryBinder.class.getName());

	public static void bindQuery(NamedQuery queryAnn, Mappings mappings, boolean isDefault) {
		if ( queryAnn == null ) return;
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		//EJBQL Query
		QueryHint[] hints = queryAnn.hints();
		String queryName = queryAnn.query();
		NamedQueryDefinition query = new NamedQueryDefinition(
				queryAnn.name(),
				queryName,
				getBoolean( queryName, "org.hibernate.cacheable", hints ),
				getString( queryName, "org.hibernate.cacheRegion", hints ),
				getTimeout( queryName, hints ),
				getInteger( queryName, "javax.persistence.lock.timeout", hints ),
				getInteger( queryName, "org.hibernate.fetchSize", hints ),
				getFlushMode( queryName, hints ),
				getCacheMode( queryName, hints ),
				getBoolean( queryName, "org.hibernate.readOnly", hints ),
				getString( queryName, "org.hibernate.comment", hints ),
				null
		);
		if ( isDefault ) {
			mappings.addDefaultQuery( query.getName(), query );
		}
		else {
			mappings.addQuery( query.getName(), query );
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", query.getName(), query.getQueryString() );
		}
	}

	public static void bindNativeQuery(NamedNativeQuery queryAnn, Mappings mappings, boolean isDefault) {
		if ( queryAnn == null ) return;
		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		NamedSQLQueryDefinition query;
		String resultSetMapping = queryAnn.resultSetMapping();
		QueryHint[] hints = queryAnn.hints();
		String queryName = queryAnn.query();
		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMapping ) ) {
			//sql result set usage
			query = new NamedSQLQueryDefinition(
					queryAnn.name(),
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
					queryAnn.name(),
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
			mappings.addDefaultSQLQuery( query.getName(), query );
		}
		else {
			mappings.addSQLQuery( query.getName(), query );
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", queryAnn.name(), queryAnn.query() );
		}
	}

	public static void bindNativeQuery(org.hibernate.annotations.NamedNativeQuery queryAnn, Mappings mappings) {
		if ( queryAnn == null ) return;
		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		NamedSQLQueryDefinition query;
		String resultSetMapping = queryAnn.resultSetMapping();
		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMapping ) ) {
			//sql result set usage
			query = new NamedSQLQueryDefinition(
					queryAnn.name(),
					queryAnn.query(),
					resultSetMapping,
					null,
					queryAnn.cacheable(),
					BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
					queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
					queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
					getFlushMode( queryAnn.flushMode() ),
					getCacheMode( queryAnn.cacheMode() ),
					queryAnn.readOnly(),
					BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment(),
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
					queryAnn.name(),
					queryAnn.query(),
					new NativeSQLQueryReturn[] { entityQueryReturn },
					null,
					queryAnn.cacheable(),
					BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
					queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
					queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
					getFlushMode( queryAnn.flushMode() ),
					getCacheMode( queryAnn.cacheMode() ),
					queryAnn.readOnly(),
					BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment(),
					null,
					queryAnn.callable()
			);
		}
		else {
			throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
		}
		mappings.addSQLQuery( query.getName(), query );
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", query.getName(), queryAnn.query() );
		}
	}

	public static void bindQueries(NamedQueries queriesAnn, Mappings mappings, boolean isDefault) {
		if ( queriesAnn == null ) return;
		for (NamedQuery q : queriesAnn.value()) {
			bindQuery( q, mappings, isDefault );
		}
	}

	public static void bindNativeQueries(NamedNativeQueries queriesAnn, Mappings mappings, boolean isDefault) {
		if ( queriesAnn == null ) return;
		for (NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, mappings, isDefault );
		}
	}

	public static void bindNativeQueries(
			org.hibernate.annotations.NamedNativeQueries queriesAnn, Mappings mappings
	) {
		if ( queriesAnn == null ) return;
		for (org.hibernate.annotations.NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, mappings );
		}
	}

	public static void bindQuery(org.hibernate.annotations.NamedQuery queryAnn, Mappings mappings) {
		if ( queryAnn == null ) return;
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		FlushMode flushMode;
		flushMode = getFlushMode( queryAnn.flushMode() );

		NamedQueryDefinition query = new NamedQueryDefinition(
				queryAnn.name(),
				queryAnn.query(),
				queryAnn.cacheable(),
				BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ? null : queryAnn.cacheRegion(),
				queryAnn.timeout() < 0 ? null : queryAnn.timeout(),
				queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize(),
				flushMode,
				getCacheMode( queryAnn.cacheMode() ),
				queryAnn.readOnly(),
				BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment(),
				null
		);

		mappings.addQuery( query.getName(), query );
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", query.getName(), query.getQueryString() );
		}
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


	public static void bindQueries(org.hibernate.annotations.NamedQueries queriesAnn, Mappings mappings) {
		if ( queriesAnn == null ) return;
		for (org.hibernate.annotations.NamedQuery q : queriesAnn.value()) {
			bindQuery( q, mappings );
		}
	}

	public static void bindSqlResultsetMappings(SqlResultSetMappings ann, Mappings mappings, boolean isDefault) {
		if ( ann == null ) return;
		for (SqlResultSetMapping rs : ann.value()) {
			//no need to handle inSecondPass
			mappings.addSecondPass( new ResultsetMappingSecondPass( rs, mappings, true ) );
		}
	}

	public static void bindSqlResultsetMapping(SqlResultSetMapping ann, Mappings mappings, boolean isDefault) {
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
			timeout = (int)Math.round(timeout.doubleValue() / 1000.0 );
		}
		else {
			// timeout is already in seconds
			timeout = getInteger( queryName, "org.hibernate.timeout", hints );
		}
		return timeout;
	}
}
