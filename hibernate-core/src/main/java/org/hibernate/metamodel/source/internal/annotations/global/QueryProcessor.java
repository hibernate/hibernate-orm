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
package org.hibernate.metamodel.source.internal.annotations.global;

import java.util.Collection;
import java.util.HashMap;

import javax.persistence.LockModeType;
import javax.persistence.ParameterMode;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.QueryHints;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.NamedStoredProcedureQueryDefinition;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * Handles processing of named queries defined via:<ul>
 *     <li>
 *       {@link javax.persistence.NamedQuery} (and
 *       {@link javax.persistence.NamedQueries})
 *     </li>
 *     <li>
 *         {@link javax.persistence.NamedNativeQuery} (and
 *         {@link javax.persistence.NamedNativeQueries})
 *     </li>
 *     <li>
 *         {@link javax.persistence.NamedStoredProcedureQuery} (and
 *         {@link javax.persistence.NamedStoredProcedureQueries})
 *     </li>
 *     <li>
 *         {@link org.hibernate.annotations.NamedQuery} (and
 *         {@link org.hibernate.annotations.NamedQueries})
 *     </li>
 *     <li>
 *         {@link org.hibernate.annotations.NamedNativeQuery} (and
 *         {@link org.hibernate.annotations.NamedNativeQueries})
 *     </li>
 * </ul>
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class QueryProcessor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryProcessor.class );

	/**
	 * Disallow direct instantiation
	 */
	private QueryProcessor() {
	}

	/**
	 * Main entry point into named query processing
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		Collection<AnnotationInstance> annotations = JandexHelper.collectionAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				JPADotNames.NAMED_QUERY,
				JPADotNames.NAMED_QUERIES
		);
		for ( AnnotationInstance query : annotations ) {
			bindNamedQuery( bindingContext, query );
		}

		annotations = JandexHelper.collectionAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				JPADotNames.NAMED_NATIVE_QUERY,
				JPADotNames.NAMED_NATIVE_QUERIES
		);
		for ( AnnotationInstance query : annotations ) {
			bindNamedNativeQuery( bindingContext, query );
		}

		annotations = JandexHelper.collectionAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				JPADotNames.NAMED_STORED_PROCEDURE_QUERY,
				JPADotNames.NAMED_STORED_PROCEDURE_QUERIES
		);
		for ( AnnotationInstance query : annotations ) {
			bindNamedStoredProcedureQuery( bindingContext, query );
		}

		annotations = JandexHelper.collectionAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				HibernateDotNames.NAMED_QUERY,
				HibernateDotNames.NAMED_QUERIES
		);
		for ( AnnotationInstance query : annotations ) {
			bindNamedQuery( bindingContext, query );
		}

		annotations = JandexHelper.collectionAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				HibernateDotNames.NAMED_NATIVE_QUERY,
				HibernateDotNames.NAMED_NATIVE_QUERIES
		);
		for ( AnnotationInstance query : annotations ) {
			bindNamedNativeQuery( bindingContext, query );
		}
	}

	/**
	 * Binds {@link javax.persistence.NamedQuery} as well as {@link org.hibernate.annotations.NamedQuery}.
	 */
	private static void bindNamedQuery(AnnotationBindingContext bindingContext, AnnotationInstance annotation) {
		final ClassLoaderService classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		final String name = JandexHelper.getValue( annotation, "name", String.class, classLoaderService );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder();
		builder.setName( name );

		final String query = JandexHelper.getValue( annotation, "query", String.class, classLoaderService );
		builder.setQuery( query );
		if ( annotation.name().equals( JPADotNames.NAMED_QUERY ) ) {
			bindJPANamedQuery( annotation, builder, name, query, bindingContext );
		} else {
			builder.setFlushMode( getFlushMode( JandexHelper.getEnumValue( annotation, "flushMode", FlushModeType.class, classLoaderService ) ) )
					.setCacheable( JandexHelper.getValue( annotation, "cacheable", Boolean.class, classLoaderService ) )
					.setCacheRegion( defaultToNull( JandexHelper.getValue( annotation, "cacheRegion", String.class, classLoaderService ) ) )
					.setFetchSize( defaultToNull( JandexHelper.getValue( annotation, "fetchSize", Integer.class, classLoaderService ) ) )
					.setTimeout( defaultToNull( JandexHelper.getValue( annotation, "timeout", Integer.class, classLoaderService ) ) )
					.setComment( JandexHelper.getValue( annotation, "comment", String.class, classLoaderService ) )
					.setCacheMode( getCacheMode( JandexHelper.getValue( annotation, "cacheMode", CacheModeType.class, classLoaderService ) ) )
					.setReadOnly( JandexHelper.getValue( annotation, "readOnly", Boolean.class, classLoaderService ) );
		}

		bindingContext.getMetadataCollector().addNamedQuery(builder.createNamedQueryDefinition());
		LOG.debugf( "Binding named query: %s => %s", name, query );
	}

	private static void bindJPANamedQuery(
			AnnotationInstance annotation,
			NamedQueryDefinitionBuilder builder,
			String name,
			String query,
			AnnotationBindingContext bindingContext){
		final ClassLoaderService classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		AnnotationInstance[] hints = JandexHelper.getValue( annotation, "hints", AnnotationInstance[].class,
				classLoaderService );

		String cacheRegion = getString( hints, QueryHints.CACHE_REGION, bindingContext );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}

		Integer timeout = getTimeout( hints, query, bindingContext );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}
		//TODO this 'javax.persistence.lock.timeout' has been mvoed to {@code AvailableSettings} in master
		//we should change this when we merge this branch back.
		Integer lockTimeout =  getInteger( hints, "javax.persistence.lock.timeout" , query, bindingContext );
		lockTimeout = defaultToNull( lockTimeout );
		
		LockOptions lockOptions = new LockOptions( LockModeConverter.convertToLockMode( JandexHelper.getEnumValue(
				annotation,
				"lockMode",
				LockModeType.class,
				classLoaderService
		) ) );
		if ( lockTimeout != null ) {
			lockOptions.setTimeOut( lockTimeout );
		}

		builder.setCacheable( getBoolean( hints, QueryHints.CACHEABLE, name, bindingContext ) )
				.setCacheRegion( cacheRegion )
				.setTimeout( timeout )
				.setLockOptions( lockOptions )
				.setFetchSize( defaultToNull( getInteger( hints, QueryHints.FETCH_SIZE, name, bindingContext ) ) )
				.setFlushMode( getFlushMode( hints, QueryHints.FLUSH_MODE, name, bindingContext ) )
				.setCacheMode( getCacheMode( hints, QueryHints.CACHE_MODE, name, bindingContext ) )
				.setReadOnly( getBoolean( hints, QueryHints.READ_ONLY, name, bindingContext ) )
				.setComment( defaultToNull( getString( hints, QueryHints.COMMENT, bindingContext ) ) )
				.setParameterTypes( null );
	}
	
	private static void bindNamedNativeQuery(AnnotationBindingContext bindingContext, AnnotationInstance annotation) {
		final ClassLoaderService classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		final String name = JandexHelper.getValue( annotation, "name", String.class, classLoaderService );
		if ( StringHelper.isEmpty( name ) ) {
			throw new AnnotationException( "A named native query must have a name when used in class or package level" );
		}
		NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder();
		builder.setName( name );

		final String query = JandexHelper.getValue( annotation, "query", String.class, classLoaderService );
		builder.setQuery( query );
		
		if ( annotation.name().equals( JPADotNames.NAMED_NATIVE_QUERY ) ) {
			bindJPANamedNativeQuery( annotation, builder, name, query, bindingContext );
			
			final String resultSetMapping = JandexHelper.getValue(
					annotation, "resultSetMapping", String.class, classLoaderService );
			if ( StringHelper.isNotEmpty( resultSetMapping ) ) {
				boolean resultSetMappingExists = bindingContext.getMetadataCollector().getResultSetMappingDefinitions().containsKey( resultSetMapping );
				if ( !resultSetMappingExists ) {
					throw new MappingException(
							String.format(
									"Named SQL Query [%s] referenced an non-existent result set mapping [%s] ",
									name,
									resultSetMapping
							)
					);
				}
				builder.setResultSetRef( resultSetMapping );
			}
			else {
				AnnotationValue annotationValue = annotation.value( "resultClass" );
				NativeSQLQueryRootReturn[] queryRoots;
				if ( annotationValue == null ) {
					// pure native scalar query
					queryRoots = new NativeSQLQueryRootReturn[0];
				}
				else {
					queryRoots = new NativeSQLQueryRootReturn[] {
							new NativeSQLQueryRootReturn(
									"alias1",
									annotationValue.asString(),
									new HashMap<String, String[]>(),
									LockMode.READ
							)
					};
				}
				builder.setQueryReturns( queryRoots );
			}
		}
		else {
			builder.setFlushMode( getFlushMode( JandexHelper.getEnumValue( annotation, "flushMode", FlushModeType.class, classLoaderService ) ) )
					.setCacheable( JandexHelper.getValue( annotation, "cacheable", Boolean.class, classLoaderService ) )
					.setCacheRegion( defaultToNull( JandexHelper.getValue( annotation, "cacheRegion", String.class, classLoaderService ) ) )
					.setFetchSize( defaultToNull( JandexHelper.getValue( annotation, "fetchSize", Integer.class, classLoaderService ) ) )
					.setTimeout( defaultToNull( JandexHelper.getValue( annotation, "timeout", Integer.class, classLoaderService ) ) )
					.setComment( JandexHelper.getValue( annotation, "comment", String.class, classLoaderService ) )
					.setCacheMode( getCacheMode( JandexHelper.getValue( annotation, "cacheMode", CacheModeType.class, classLoaderService ) ) )
					.setReadOnly( JandexHelper.getValue( annotation, "readOnly", Boolean.class, classLoaderService ) )
					.setCallable( JandexHelper.getValue( annotation, "callable", Boolean.class, classLoaderService ) )
					.setQueryReturns( new NativeSQLQueryRootReturn[0] );
		}

		bindingContext.getMetadataCollector().addNamedNativeQuery(builder.createNamedQueryDefinition());
		LOG.debugf( "Binding named query: %s => %s", name, query );
	}

	private static void bindJPANamedNativeQuery(
			AnnotationInstance annotation,
			NamedSQLQueryDefinitionBuilder builder,
			String name,
			String query,
			AnnotationBindingContext bindingContext){
		final ClassLoaderService classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		AnnotationInstance[] hints = JandexHelper.getValue( annotation, "hints", AnnotationInstance[].class,
				classLoaderService );

		String cacheRegion = getString( hints, QueryHints.CACHE_REGION, bindingContext );
		if ( StringHelper.isEmpty( cacheRegion ) ) {
			cacheRegion = null;
		}

		Integer timeout = getTimeout( hints, query, bindingContext );
		if ( timeout != null && timeout < 0 ) {
			timeout = null;
		}
		
		builder.setCacheable( getBoolean( hints, QueryHints.CACHEABLE, name, bindingContext ) )
				.setCacheRegion( cacheRegion )
				.setTimeout( timeout )
				.setFetchSize( defaultToNull( getInteger( hints, QueryHints.FETCH_SIZE, name, bindingContext ) ) )
				.setFlushMode( getFlushMode( hints, QueryHints.FLUSH_MODE, name, bindingContext ) )
				.setCacheMode( getCacheMode( hints, QueryHints.CACHE_MODE, name, bindingContext ) )
				.setReadOnly( getBoolean( hints, QueryHints.READ_ONLY, name, bindingContext ) )
				.setComment( defaultToNull( getString( hints, QueryHints.COMMENT, bindingContext ) ) )
				.setParameterTypes( null )
				.setCallable( getBoolean( hints, QueryHints.CALLABLE, name, bindingContext ) );
	}

	public static FlushMode getFlushMode(FlushModeType flushModeType) {
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

	private static void bindNamedStoredProcedureQuery(
			AnnotationBindingContext bindingContext,
			AnnotationInstance query) {
		final String name = query.value( "name" ).asString();
		final String procedureName = query.value( "procedureName" ).asString();
		LOG.debugf( "Starting binding of @NamedStoredProcedureQuery(name=%s, procedureName=%s)", name, procedureName );
		NamedStoredProcedureQueryDefinition.Builder builder = new NamedStoredProcedureQueryDefinition.Builder(
				name,
				procedureName
		);

		final AnnotationInstance[] parameterAnnotations = JandexHelper.extractAnnotationsValue(
				query,
				"parameters"
		);
		if ( parameterAnnotations != null && parameterAnnotations.length > 0 ) {
			for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
				final AnnotationValue pNameValue = parameterAnnotation.value( "name" );
				final String pName;
				if ( pNameValue == null ) {
					pName = null;
				}
				else {
					pName = StringHelper.nullIfEmpty( pNameValue.asString() );
				}

				final AnnotationValue pModeValue = parameterAnnotation.value( "mode" );
				final ParameterMode pMode;
				if ( pModeValue == null ) {
					pMode = ParameterMode.IN;
				}
				else {
					final String pModeName = StringHelper.nullIfEmpty( pModeValue.asEnum() );
					if ( pModeName == null ) {
						pMode = ParameterMode.IN;
					}
					else {
						pMode = ParameterMode.valueOf( pModeName );
					}
				}

				final AnnotationValue javaTypeValue = parameterAnnotation.value( "type" );
				final String pJavaType;
				if ( javaTypeValue == null ) {
					pJavaType = null;
				}
				else {
					pJavaType = StringHelper.nullIfEmpty( javaTypeValue.asString() );
				}

				builder.addParameter( pName, pMode, pJavaType );
			}
		}

		final AnnotationInstance[] hintAnnotations = JandexHelper.extractAnnotationsValue(
				query,
				"hints"
		);
		if ( hintAnnotations != null && hintAnnotations.length > 0 ) {
			for ( AnnotationInstance hintAnnotation : hintAnnotations ) {
				builder.addHint(
						hintAnnotation.value( "name" ).asString(),
						hintAnnotation.value( "value" ).asString()
				);
			}
		}

		final AnnotationValue resultClassesValue = query.value( "resultClasses" );
		if ( resultClassesValue != null ) {
			final String[] resultClassNames = resultClassesValue.asStringArray();
			if ( resultClassNames != null ) {
				for ( String resultClassName : resultClassNames ) {
					builder.addResultClassName( resultClassName );
				}
			}
		}

		final AnnotationValue resultSetMappingsValue = query.value( "resultSetMappings" );
		if ( resultSetMappingsValue != null ) {
			final String[] resultSetMappingNames = resultSetMappingsValue.asStringArray();
			if ( resultSetMappingNames != null ) {
				for ( String resultSetMappingName : resultSetMappingNames ) {
					builder.addResultSetMappingName( resultSetMappingName );
				}
			}
		}

		bindingContext.getMetadataCollector().addNamedStoredProcedureQueryDefinition(
				builder.buildDefinition()
		);
	}

	private static boolean getBoolean(AnnotationInstance[] hints, String element, String query, AnnotationBindingContext bindingContext) {
		String val = getString( hints, element, bindingContext );
		if ( val == null || val.equalsIgnoreCase( "false" ) ) {
			return false;
		}
		if ( val.equalsIgnoreCase( "true" ) ) {
			return true;
		}
		throw new AnnotationException( "Not a boolean in hint: " + query + ":" + element );
	}

	private static CacheMode getCacheMode(AnnotationInstance[] hints, String element, String query, AnnotationBindingContext bindingContext) {
		String val = getString( hints, element, bindingContext );
		if ( val == null ) {
			return null;
		}
		try {
			return CacheMode.valueOf( val.toUpperCase() );
		}
		catch ( IllegalArgumentException e ) {
			throw new AnnotationException( "Unknown CacheMode in hint: " + query + ":" + element );
		}
	}

	private static FlushMode getFlushMode(AnnotationInstance[] hints, String element, String query, AnnotationBindingContext bindingContext) {
		String val = getString( hints, element, bindingContext );
		if ( val == null ) {
			return null;
		}
		try {
			return FlushMode.valueOf( val.toUpperCase() );
		}
		catch ( IllegalArgumentException e ) {
			throw new AnnotationException( "Unknown FlushMode in hint: " + query + ":" + element );
		}
	}

	private static Integer getInteger(AnnotationInstance[] hints, String element, String query, AnnotationBindingContext bindingContext) {
		String val = getString( hints, element, bindingContext );
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

	private static String getString(AnnotationInstance[] hints, String element, AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		for ( AnnotationInstance hint : hints ) {
			if ( element.equals( JandexHelper.getValue( hint, "name", String.class, classLoaderService ) ) ) {
				return JandexHelper.getValue( hint, "value", String.class, classLoaderService );
			}
		}
		return null;
	}

	private static Integer getTimeout(AnnotationInstance[] hints, String query, AnnotationBindingContext bindingContext) {
		Integer timeout = getInteger( hints, QueryHints.TIMEOUT_JPA, query, bindingContext );
		if ( timeout == null ) {
			return getInteger( hints, QueryHints.TIMEOUT_HIBERNATE, query, bindingContext ); // timeout is already in seconds
		}
		return ( ( timeout + 500 ) / 1000 ); // convert milliseconds to seconds (rounded)
	}
	
	private static String defaultToNull( String s ) {
		return StringHelper.isEmpty( s ) ? null : s;
	}
	
	private static Integer defaultToNull( Integer i ) {
		return i == null || i < 0 ? null : i;
	}
}
