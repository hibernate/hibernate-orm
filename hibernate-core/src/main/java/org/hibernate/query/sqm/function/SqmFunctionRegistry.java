/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.sqm.produce.function.NamedFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.VarArgsFunctionDescriptorBuilder;

import org.jboss.logging.Logger;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Defines a registry for SqmFunctionDescriptor instances
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("UnusedReturnValue")
@Incubating
public class SqmFunctionRegistry {
	private static final Logger log = Logger.getLogger( SqmFunctionRegistry.class );

	private final Map<String, SqmFunctionDescriptor> functionMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );
	private final Map<String,String> alternateKeyMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );

	public SqmFunctionRegistry() {
		log.tracef( "SqmFunctionRegistry created" );
	}

	public Map<String, SqmFunctionDescriptor> getFunctions() {
		return functionMap;
	}

	/**
	 * Find a SqmFunctionTemplate by name.  Returns {@code null} if
	 * no such function is found.
	 */
	public SqmFunctionDescriptor findFunctionDescriptor(String functionName) {
		SqmFunctionDescriptor found = null;

		final String alternateKeyResolution = alternateKeyMap.get( functionName );
		if ( alternateKeyResolution != null ) {
			found = functionMap.get( alternateKeyResolution );
		}

		if ( found == null ) {
			found = functionMap.get( functionName );
		}

		return found;
	}

	public SqmFunctionDescriptor getFunctionDescriptor(String functionName) {
		final SqmFunctionDescriptor functionDescriptor = findFunctionDescriptor( functionName );
		if ( functionDescriptor == null ) {
			throw new UnknownFunctionException( "No SqmFunctionDescriptor registered as '" + functionName + "'" );
		}
		return functionDescriptor;
	}

	/**
	 * Register a function descriptor by name
	 */
	public SqmFunctionDescriptor register(String registrationKey, SqmFunctionDescriptor function) {
		final SqmFunctionDescriptor priorRegistration = functionMap.put( registrationKey, function );
		log.debugf(
				"Registered SqmFunctionTemplate [%s] under '%s'.  Prior registration = [%s]",
				function,
				registrationKey,
				priorRegistration
		);

		return function;
	}

	/**
	 * Register a pattern-based descriptor by name.  Shortcut for building the descriptor via
	 * {@link #patternDescriptorBuilder} accepting its defaults.
	 */
	public SqmFunctionDescriptor registerPattern(String name, String pattern) {
		return patternDescriptorBuilder( pattern ).register( name );
	}

	/**
	 * Register a pattern-based descriptor by name and invariant return type.  Shortcut for building the descriptor
	 * via {@link #patternDescriptorBuilder} accepting its defaults.
	 */
	public SqmFunctionDescriptor registerPattern(String name, String pattern, BasicValuedMapping returnType) {
		return patternDescriptorBuilder( pattern )
				.setInvariantType( returnType )
				.register( name );
	}

	/**
	 * Get a builder for creating and registering a pattern-based function descriptor.
	 * @param pattern The pattern defining the the underlying function call
	 *
	 * @return The builder
	 */
	public PatternFunctionDescriptorBuilder patternDescriptorBuilder(String pattern) {
		return new PatternFunctionDescriptorBuilder( this, pattern );
	}

	/**
	 * Register a descriptor by name.  Shortcut for building a descriptor via {@link #namedDescriptorBuilder} using the
	 * passed name as both the registration key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNamed(String name) {
		return namedDescriptorBuilder( name ).build();
	}

	/**
	 * Register a named descriptor by name and invariant return type.  Shortcut for building
	 * a descriptor via {@link #namedDescriptorBuilder} using the passed name as both the
	 * registration key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNamed(String name, BasicValuedMapping returnType) {
		return namedDescriptorBuilder( name ).setInvariantType( returnType ).build();
	}

	/**
	 * Get a builder for creating and registering a name-based function descriptor using the passed name as
	 * both the registration key and underlying SQL function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedDescriptorBuilder(String name) {
		return new NamedFunctionDescriptorBuilder( this, name );
	}

	public NamedFunctionDescriptorBuilder noArgsBuilder(String name) {
		return namedDescriptorBuilder( name )
				.setExactArgumentCount( 0 );
	}

	public VarArgsFunctionDescriptorBuilder varArgsBuilder(String begin, String sep, String end) {
		return new VarArgsFunctionDescriptorBuilder( this, begin, sep, end );
	}

	/**
	 * Specialized registration method for registering a named template for functions
	 * expecting zero arguments.  Short-cut for building a named template via
	 * {@link #namedDescriptorBuilder} specifying zero arguments and accepting the
	 * rest of the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNoArgs(String name) {
		return noArgsBuilder( name ).build();
	}

	public SqmFunctionDescriptor registerNoArgs(String name, BasicValuedMapping returnType) {
		return noArgsBuilder( name )
				.setInvariantType( returnType )
				.build();
	}

	public SqmFunctionDescriptor registerVarArgs(
			String registrationKey,
			BasicValuedMapping returnType,
			String begin,
			String sep,
			String end) {
		return varArgsBuilder( begin, sep, end )
				.setInvariantType( returnType )
				.register( registrationKey );
	}

	public SqmFunctionDescriptor wrapInJdbcEscape(String registrationKey, SqmFunctionDescriptor wrapped) {
		final JdbcEscapeFunctionDescriptor wrapper = new JdbcEscapeFunctionDescriptor( wrapped );
		register( registrationKey, wrapper );
		return wrapper;
	}

	public void registerAlternateKey(String alternateKey, String mappedKey) {
		log.debugf( "Registering alternate key : %s -> %s", alternateKey, mappedKey );
		alternateKeyMap.put( alternateKey, mappedKey );
	}

	/**
	 * Register a binary/ternary function.
	 *
	 * i.e. a function which accepts 2-3 arguments.
	 */
	public void registerBinaryTernaryPattern(
			String name,
			BasicValuedMapping type,
			String pattern2,
			String pattern3) {
		registerPatterns( name, type, null, null, pattern2, pattern3 );
	}

	private void registerPatterns(
			String name,
			BasicValuedMapping type,
			String... patterns) {
		final SqmFunctionDescriptor[] templates = new SqmFunctionDescriptor[patterns.length];
		for ( int i = 0; i < patterns.length; i++ ) {
			final String pattern = patterns[i];
			if ( pattern != null ) {
				templates[i] = patternDescriptorBuilder( pattern )
						.setExactArgumentCount( i )
						.setInvariantType( type )
						.build();
			}
		}

		register( name, new MultiPatternSqmFunctionDescriptor( templates ) );
	}

	/**
	 * Overlay the functions registered here on top of the
	 * incoming registry, potentially overriding its registrations
	 */
	public void overlay(SqmFunctionRegistry registryToOverly) {
		// NOTE : done in this "direction" as it is easier to access the
		//		functionMap directly in performing this operation
		functionMap.forEach( registryToOverly::register );
		alternateKeyMap.forEach( registryToOverly::registerAlternateKey );
	}

	public void close() {
		functionMap.clear();
		alternateKeyMap.clear();
	}
}
