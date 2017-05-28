/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;

import org.jboss.logging.Logger;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Defines a registry for SQLFunction instances
 *
 * @author Steve Ebersole
 */
public class SqmFunctionRegistry {
	private static final Logger log = Logger.getLogger( SqmFunctionRegistry.class );

	private final Map<String,SqmFunctionTemplate> functionMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );
	private final Map<String,String> alternateKeyMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );

	public SqmFunctionRegistry() {
		log.tracef( "SqmFunctionRegistry created" );
	}

	public SqmFunctionRegistry register(String registrationKey, SqmFunctionTemplate function) {
		final SqmFunctionTemplate priorRegistration = functionMap.put( registrationKey, function );
		log.debugf(
				"Registered SqmFunctionTemplate [%s] under %s; prior registration was %s",
				function,
				registrationKey,
				priorRegistration
		);

		return this;
	}

	public SqmFunctionRegistry registerPattern(String name, String pattern) {
		patternTemplateBuilder( name, pattern ).register();
		return this;
	}

	public PatternFunctionTemplateBuilder patternTemplateBuilder(String registrationKey, String pattern) {
		return new PatternFunctionTemplateBuilder( this, registrationKey, pattern );
	}

	public SqmFunctionRegistry registerNamed(String name) {
		namedTemplateBuilder( name ).register();
		return this;
	}

	public SqmFunctionRegistry registerNamed(String name, AllowableFunctionReturnType returnType) {
		namedTemplateBuilder( name ).register();
		return this;
	}

	public NamedFunctionTemplateBuilder namedTemplateBuilder(String registrationKey, String name) {
		return new NamedFunctionTemplateBuilder( this, registrationKey, name );
	}

	public NamedFunctionTemplateBuilder namedTemplateBuilder(String name) {
		return new NamedFunctionTemplateBuilder( this, name, name );
	}

	/**
	 * Overlay (put on top) the functions registered here on top of the
	 * incoming registry, potentially overidding its registrations
	 */
	public void overlay(SqmFunctionRegistry registryToOverly) {
		functionMap.forEach( registryToOverly::register );
	}

	/**
	 * Constructs a SQLFunctionRegistry
	 *
	 * @param dialect The dialect
	 * @param userFunctionMap Any application-supplied function definitions
	 */
	public SqmFunctionRegistry(Dialect dialect, Map<String, SqmFunctionTemplate> userFunctionMap) {
		// Apply the Dialect functions first
		functionMap.putAll( dialect.getFunctions() );
		// so that user supplied functions "override" them
		if ( userFunctionMap != null ) {
			functionMap.putAll( userFunctionMap );
		}
	}

	/**
	 * Find a SQLFunction by name
	 *
	 * @param functionName The name of the function to locate
	 *
	 * @return The located function, maye return {@code null}
	 */
	public SqmFunctionTemplate findSQLFunction(String functionName) {
		return functionMap.get( functionName );
	}

	/**
	 * Does this registry contain the named function
	 *
	 * @param functionName The name of the function to attempt to locate
	 *
	 * @return {@code true} if the registry contained that function
	 */
	@SuppressWarnings("UnusedDeclaration")
	public boolean hasFunction(String functionName) {
		return functionMap.containsKey( functionName );
	}

	public void registerAlternateKey(String alternateKey, String mappedKey) {
		log.debugf( "Registering alternate key : %s -> %s", alternateKey, mappedKey );
		alternateKeyMap.put( alternateKey, mappedKey );
	}
}
