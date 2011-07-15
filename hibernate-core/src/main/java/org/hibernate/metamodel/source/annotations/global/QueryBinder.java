/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.global;

import java.util.HashMap;
import java.util.List;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.QueryHints;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Binds {@link NamedQuery}, {@link NamedQueries}, {@link NamedNativeQuery}, {@link NamedNativeQueries},
 * {@link org.hibernate.annotations.NamedQuery}, {@link org.hibernate.annotations.NamedQueries},
 * {@link org.hibernate.annotations.NamedNativeQuery}, and {@link org.hibernate.annotations.NamedNativeQueries}.
 *
 * @author Hardy Ferentschik
 */
public class QueryBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			QueryBinder.class.getName()
	);

	private QueryBinder() {
	}

	/**
	 * Binds all {@link NamedQuery}, {@link NamedQueries}, {@link NamedNativeQuery}, {@link NamedNativeQueries},
	 * {@link org.hibernate.annotations.NamedQuery}, {@link org.hibernate.annotations.NamedQueries},
	 * {@link org.hibernate.annotations.NamedNativeQuery}, and {@link org.hibernate.annotations.NamedNativeQueries}
	 * annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		List<AnnotationInstance> annotations = bindingContext.getIndex().getAnnotations( JPADotNames.NAMED_QUERY );
		for ( AnnotationInstance query : annotations ) {
			bindNamedQuery( bindingContext.getMetadataImplementor(), query );
		}

		annotations = bindingContext.getIndex().getAnnotations( JPADotNames.NAMED_QUERIES );
		for ( AnnotationInstance queries : annotations ) {
			for ( AnnotationInstance query : JandexHelper.getValue( queries, "value", AnnotationInstance[].class ) ) {
				bindNamedQuery( bindingContext.getMetadataImplementor(), query );
			}
		}

		annotations = bindingContext.getIndex().getAnnotations( JPADotNames.NAMED_NATIVE_QUERY );
		for ( AnnotationInstance query : annotations ) {
			bindNamedNativeQuery( bindingContext.getMetadataImplementor(), query );
		}

		annotations = bindingContext.getIndex().getAnnotations( JPADotNames.NAMED_NATIVE_QUERIES );
		for ( AnnotationInstance queries : annotations ) {
			for ( AnnotationInstance query : JandexHelper.getValue( queries, "value", AnnotationInstance[].class ) ) {
				bindNamedNativeQuery( bindingContext.getMetadataImplementor(), query );
			}
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.NAMED_QUERY );
		for ( AnnotationInstance query : annotations ) {
			bindNamedQuery( bindingContext.getMetadataImplementor(), query );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.NAMED_QUERIES );
		for ( AnnotationInstance queries : annotations ) {
			for ( AnnotationInstance query : JandexHelper.getValue( queries, "value", AnnotationInstance[].class ) ) {
				bindNamedQuery( bindingContext.getMetadataImplementor(), query );
			}
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERY );
		for ( AnnotationInstance query : annotations ) {
			bindNamedNativeQuery( bindingContext.getMetadataImplementor(), query );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.NAMED_NATIVE_QUERIES );
		for ( AnnotationInstance queries : annotations ) {
			for ( AnnotationInstance query : JandexHelper.getValue( queries, "value", AnnotationInstance[].class ) ) {
				bindNamedNativeQuery( bindingContext.getMetadataImplementor(), query );
			}
		}
	}

	/**
	 * Binds {@link javax.persistence.NamedQuery} as well as {@link org.hibernate.annotations.NamedQuery}.
	 *
	 * @param metadata the current metadata
	 * @param annotation the named query annotation
	 */
	private static void bindNamedQuery(MetadataImplementor metadata, AnnotationInstance annotation) {
		String name = JandexHelper.getValue( annotation, "name", String.class );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		String query = JandexHelper.getValue( annotation, "query", String.class );

		AnnotationInstance[] hints = JandexHelper.getValue( annotation, "hints", AnnotationInstance[].class );

		String cacheRegion = getString( hints, QueryHints.CACHE_REGION );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}

		Integer timeout = getTimeout( hints, query );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}

		Integer fetchSize = getInteger( hints, QueryHints.FETCH_SIZE, name );
		if ( fetchSize != null && fetchSize < 0 ) {
			fetchSize = null;
		}

		String comment = getString( hints, QueryHints.COMMENT );
		if ( StringHelper.isEmpty( comment ) ) {
			comment = null;
		}

		metadata.addNamedQuery(
				new NamedQueryDefinition(
						name,
						query, getBoolean( hints, QueryHints.CACHEABLE, name ), cacheRegion,
						timeout, fetchSize, getFlushMode( hints, QueryHints.FLUSH_MODE, name ),
						getCacheMode( hints, QueryHints.CACHE_MODE, name ),
						getBoolean( hints, QueryHints.READ_ONLY, name ), comment, null
				)
		);
		LOG.debugf( "Binding named query: %s => %s", name, query );
	}

	private static void bindNamedNativeQuery(MetadataImplementor metadata, AnnotationInstance annotation) {
		String name = JandexHelper.getValue( annotation, "name", String.class );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named native query must have a name when used in class or package level" );
		}

		String query = JandexHelper.getValue( annotation, "query", String.class );

		String resultSetMapping = JandexHelper.getValue( annotation, "resultSetMapping", String.class );

		AnnotationInstance[] hints = JandexHelper.getValue( annotation, "hints", AnnotationInstance[].class );

		boolean cacheable = getBoolean( hints, "org.hibernate.cacheable", name );
		String cacheRegion = getString( hints, QueryHints.CACHE_REGION );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}

		Integer timeout = getTimeout( hints, query );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}

		Integer fetchSize = getInteger( hints, QueryHints.FETCH_SIZE, name );
		if ( fetchSize != null && fetchSize < 0 ) {
			fetchSize = null;
		}

		FlushMode flushMode = getFlushMode( hints, QueryHints.FLUSH_MODE, name );
		CacheMode cacheMode = getCacheMode( hints, QueryHints.CACHE_MODE, name );

		boolean readOnly = getBoolean( hints, QueryHints.READ_ONLY, name );

		String comment = getString( hints, QueryHints.COMMENT );
		if ( StringHelper.isEmpty( comment ) ) {
			comment = null;
		}

		boolean callable = getBoolean( hints, QueryHints.CALLABLE, name );
		NamedSQLQueryDefinition def;
		if ( StringHelper.isNotEmpty( resultSetMapping ) ) {
			def = new NamedSQLQueryDefinition(
					name,
					query, resultSetMapping, null, cacheable,
					cacheRegion, timeout, fetchSize,
					flushMode, cacheMode, readOnly, comment,
					null, callable
			);
		}
		else {
			AnnotationValue annotationValue = annotation.value( "resultClass" );
			if ( annotationValue == null ) {
				throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
			}
			NativeSQLQueryRootReturn queryRoots[] = new NativeSQLQueryRootReturn[] {
					new NativeSQLQueryRootReturn(
							"alias1",
							annotationValue.asString(),
							new HashMap<String, String[]>(),
							LockMode.READ
					)
			};
			def = new NamedSQLQueryDefinition(
					name,
					query,
					queryRoots,
					null,
					cacheable,
					cacheRegion,
					timeout,
					fetchSize,
					flushMode,
					cacheMode,
					readOnly,
					comment,
					null,
					callable
			);
		}
		metadata.addNamedNativeQuery( def );
		LOG.debugf( "Binding named native query: %s => %s", name, query );
	}

	private static boolean getBoolean(AnnotationInstance[] hints, String element, String query) {
		String val = getString( hints, element );
		if ( val == null || val.equalsIgnoreCase( "false" ) ) {
			return false;
		}
		if ( val.equalsIgnoreCase( "true" ) ) {
			return true;
		}
		throw new AnnotationException( "Not a boolean in hint: " + query + ":" + element );
	}

	private static CacheMode getCacheMode(AnnotationInstance[] hints, String element, String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		if ( val.equalsIgnoreCase( CacheMode.GET.toString() ) ) {
			return CacheMode.GET;
		}
		if ( val.equalsIgnoreCase( CacheMode.IGNORE.toString() ) ) {
			return CacheMode.IGNORE;
		}
		if ( val.equalsIgnoreCase( CacheMode.NORMAL.toString() ) ) {
			return CacheMode.NORMAL;
		}
		if ( val.equalsIgnoreCase( CacheMode.PUT.toString() ) ) {
			return CacheMode.PUT;
		}
		if ( val.equalsIgnoreCase( CacheMode.REFRESH.toString() ) ) {
			return CacheMode.REFRESH;
		}
		throw new AnnotationException( "Unknown CacheMode in hint: " + query + ":" + element );
	}

	private static FlushMode getFlushMode(AnnotationInstance[] hints, String element, String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		if ( val.equalsIgnoreCase( FlushMode.ALWAYS.toString() ) ) {
			return FlushMode.ALWAYS;
		}
		else if ( val.equalsIgnoreCase( FlushMode.AUTO.toString() ) ) {
			return FlushMode.AUTO;
		}
		else if ( val.equalsIgnoreCase( FlushMode.COMMIT.toString() ) ) {
			return FlushMode.COMMIT;
		}
		else if ( val.equalsIgnoreCase( FlushMode.NEVER.toString() ) ) {
			return FlushMode.MANUAL;
		}
		else if ( val.equalsIgnoreCase( FlushMode.MANUAL.toString() ) ) {
			return FlushMode.MANUAL;
		}
		else {
			throw new AnnotationException( "Unknown FlushMode in hint: " + query + ":" + element );
		}
	}

	private static Integer getInteger(AnnotationInstance[] hints, String element, String query) {
		String val = getString( hints, element );
		if ( val == null ) {
			return null;
		}
		try {
			return Integer.decode( val );
		}
		catch ( NumberFormatException nfe ) {
			throw new AnnotationException( "Not an integer in hint: " + query + ":" + element, nfe );
		}
	}

	private static String getString(AnnotationInstance[] hints, String element) {
		for ( AnnotationInstance hint : hints ) {
			if ( element.equals( JandexHelper.getValue( hint, "name", String.class ) ) ) {
				return JandexHelper.getValue( hint, "value", String.class );
			}
		}
		return null;
	}

	private static Integer getTimeout(AnnotationInstance[] hints, String query) {
		Integer timeout = getInteger( hints, QueryHints.TIMEOUT_JPA, query );
		if ( timeout == null ) {
			return getInteger( hints, QueryHints.TIMEOUT_HIBERNATE, query ); // timeout is already in seconds
		}
		return ( ( timeout + 500 ) / 1000 ); // convert milliseconds to seconds (rounded)
	}
}
