/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.Map;
import java.util.TreeMap;

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

	/**
	 * Find a SqmFunctionTemplate by name.  Returns {@code null} if
	 * no such function is found.
	 */
	public SqmFunctionTemplate findFunctionTemplate(String functionName) {
		SqmFunctionTemplate found = functionMap.get( functionName );

		if ( found == null ) {
			final String alternateKeyResolution = alternateKeyMap.get( functionName );
			if ( alternateKeyResolution != null ) {
				found = functionMap.get( alternateKeyResolution );
			}
		}

		return found;
	}

	/**
	 * Register a function template by name
	 */
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

	/**
	 * Register a pattern-based template by name.  Shortcut for building the template
	 * via {@link #patternTemplateBuilder} accepting its defaults.
	 */
	public SqmFunctionRegistry registerPattern(String name, String pattern) {
		patternTemplateBuilder( name, pattern ).register();
		return this;
	}

	/**
	 * Get a builder for creating and registering a pattern-based function template.
	 *
	 * @param registrationKey The name under which the template will get registered
	 * @param pattern The pattern defining the the underlying function call
	 *
	 * @return The builder
	 */
	public PatternFunctionTemplateBuilder patternTemplateBuilder(String registrationKey, String pattern) {
		return new PatternFunctionTemplateBuilder( this, registrationKey, pattern );
	}

	/**
	 * Register a named template by name.  Shortcut for building a template via
	 * {@link #namedTemplateBuilder} using the passed name as both the registration
	 * key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionRegistry registerNamed(String name) {
		namedTemplateBuilder( name ).register();
		return this;
	}

	/**
	 * Register a named template by name and invariant return type.  Shortcut for building
	 * a template via {@link #namedTemplateBuilder} using the passed name as both the
	 * registration key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionRegistry registerNamed(String name, AllowableFunctionReturnType returnType) {
		namedTemplateBuilder( name ).setInvariantType( returnType ).register();
		return this;
	}

	/**
	 * Get a builder for creating and registering a name-based function template
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionTemplateBuilder namedTemplateBuilder(String name) {
		return namedTemplateBuilder( name, name );
	}

	/**
	 * Get a builder for creating and registering a name-based function template.
	 *
	 * @param registrationKey The name under which the template will get registered
	 * @param name The underlying SQL function name to use
	 *
	 * @return The builder
	 */
	public NamedFunctionTemplateBuilder namedTemplateBuilder(String registrationKey, String name) {
		return new NamedFunctionTemplateBuilder( this, registrationKey, name );
	}

	public NamedFunctionTemplateBuilder noArgsBuilder(String name) {
		return noArgsBuilder( name, name );
	}

	public NamedFunctionTemplateBuilder noArgsBuilder(String registrationKey, String name) {
		return namedTemplateBuilder( registrationKey, name )
				.setExactArgumentCount( 0 );
	}

	/**
	 * Specialized registration method for registering a named template for functions
	 * expecting zero arguments.  Short-cut for building a named template via
	 * {@link #namedTemplateBuilder} specifying zero arguments and accepting the
	 * rest of the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionRegistry registerNoArgs(String name) {
		return registerNoArgs( name, name );
	}

	public SqmFunctionRegistry registerNoArgs(String registrationKey, String name) {
		return noArgsBuilder( registrationKey, name ).register();
	}

	public SqmFunctionRegistry registerNoArgs(String name, AllowableFunctionReturnType returnType) {
		return registerNoArgs( name, name, returnType );
	}

	public SqmFunctionRegistry registerNoArgs(String registrationKey, String name, AllowableFunctionReturnType returnType) {
		return noArgsBuilder( registrationKey, name )
				.setInvariantType( returnType )
				.register();
	}

	public void registerAlternateKey(String alternateKey, String mappedKey) {
		log.debugf( "Registering alternate key : %s -> %s", alternateKey, mappedKey );
		alternateKeyMap.put( alternateKey, mappedKey );
	}

	/**
	 * Overlay (put on top) the functions registered here on top of the
	 * incoming registry, potentially overidding its registrations
	 */
	public void overlay(SqmFunctionRegistry registryToOverly) {
		// NOTE : done in this "direction" as it is easier to access the
		//		functionMap directly in performing this operation
		functionMap.forEach( registryToOverly::register );
	}
}
