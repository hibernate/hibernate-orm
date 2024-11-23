/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.hibernate.internal.util.collections.CaseInsensitiveDictionary;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.NamedFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.NamedSetReturningFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Defines a registry for {@link SqmFunctionDescriptor} instances.
 * <p>
 * The {@code SqmFunctionRegistry} may be configured by a {@link org.hibernate.boot.model.FunctionContributor}.
 *
 * @see org.hibernate.boot.model.FunctionContributor
 * @see org.hibernate.boot.model.FunctionContributions
 *
 * @author Steve Ebersole
 */
public class SqmFunctionRegistry {
	private static final Logger log = Logger.getLogger( SqmFunctionRegistry.class );

	private final CaseInsensitiveDictionary<SqmFunctionDescriptor> functionMap = new CaseInsensitiveDictionary<>();
	private final CaseInsensitiveDictionary<SqmSetReturningFunctionDescriptor> setReturningFunctionMap = new CaseInsensitiveDictionary<>();
	private final CaseInsensitiveDictionary<String> alternateKeyMap = new CaseInsensitiveDictionary<>();

	public SqmFunctionRegistry() {
		log.trace( "SqmFunctionRegistry created" );
	}

	public Set<String> getValidFunctionKeys() {
		return functionMap.unmodifiableKeySet();
	}

	/**
	 * Useful for diagnostics - not efficient: do not use in production code.
	 *
	 * @return
	 */
	public Stream<Map.Entry<String, SqmFunctionDescriptor>> getFunctionsByName() {
		final Map<String, SqmFunctionDescriptor> sortedFunctionMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );
		for ( Map.Entry<String, SqmFunctionDescriptor> e : functionMap.unmodifiableEntrySet() ) {
			sortedFunctionMap.put( e.getKey(), e.getValue() );
		}
		for ( Map.Entry<String, String> e : alternateKeyMap.unmodifiableEntrySet() ) {
			sortedFunctionMap.put( e.getKey(), functionMap.get( e.getValue() ) );
		}
		return sortedFunctionMap.entrySet().stream();
	}

	/**
	 * Useful for diagnostics - not efficient: do not use in production code.
	 *
	 * @return
	 */
	public Stream<Map.Entry<String, SqmSetReturningFunctionDescriptor>> getSetReturningFunctionsByName() {
		final Map<String, SqmSetReturningFunctionDescriptor> sortedFunctionMap = new TreeMap<>( CASE_INSENSITIVE_ORDER );
		for ( Map.Entry<String, SqmSetReturningFunctionDescriptor> e : setReturningFunctionMap.unmodifiableEntrySet() ) {
			sortedFunctionMap.put( e.getKey(), e.getValue() );
		}
		for ( Map.Entry<String, String> e : alternateKeyMap.unmodifiableEntrySet() ) {
			sortedFunctionMap.put( e.getKey(), setReturningFunctionMap.get( e.getValue() ) );
		}
		return sortedFunctionMap.entrySet().stream();
	}

	/**
	 * Find a {@link SqmFunctionDescriptor} by name.
	 * Returns {@code null} if no such function is found.
	 */
	public @Nullable SqmFunctionDescriptor findFunctionDescriptor(String functionName) {
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

	/**
	 * Find a {@link SqmSetReturningFunctionDescriptor} by name.
	 * Returns {@code null} if no such function is found.
	 */
	public @Nullable SqmSetReturningFunctionDescriptor findSetReturningFunctionDescriptor(String functionName) {
		SqmSetReturningFunctionDescriptor found = null;

		final String alternateKeyResolution = alternateKeyMap.get( functionName );
		if ( alternateKeyResolution != null ) {
			found = setReturningFunctionMap.get( alternateKeyResolution );
		}

		if ( found == null ) {
			found = setReturningFunctionMap.get( functionName );
		}

		return found;
	}

	/**
	 * Register a function descriptor by name
	 */
	public SqmFunctionDescriptor register(String registrationKey, SqmFunctionDescriptor function) {
		final SqmFunctionDescriptor priorRegistration = functionMap.put( registrationKey, function );
		log.debugf(
				"Registered SqmFunctionTemplate [%s] under %s; prior registration was %s",
				function,
				registrationKey,
				priorRegistration
		);
		alternateKeyMap.remove( registrationKey );
		return function;
	}

	/**
	 * Register a set returning function descriptor by name
	 */
	public SqmSetReturningFunctionDescriptor register(String registrationKey, SqmSetReturningFunctionDescriptor function) {
		final SqmSetReturningFunctionDescriptor priorRegistration = setReturningFunctionMap.put( registrationKey, function );
		log.debugf(
				"Registered SqmSetReturningFunctionTemplate [%s] under %s; prior registration was %s",
				function,
				registrationKey,
				priorRegistration
		);
		alternateKeyMap.remove( registrationKey );
		return function;
	}

	/**
	 * Register a pattern-based descriptor by name.  Shortcut for building the descriptor
	 * via {@link #patternDescriptorBuilder} accepting its defaults.
	 */
	public SqmFunctionDescriptor registerPattern(String name, String pattern) {
		return patternDescriptorBuilder( name, pattern ).register();
	}

	/**
	 * Register a pattern-based descriptor by name and invariant return type.  Shortcut for building the descriptor
	 * via {@link #patternDescriptorBuilder} accepting its defaults.
	 */
	public SqmFunctionDescriptor registerPattern(String name, String pattern, BasicType returnType) {
		return patternDescriptorBuilder( name, pattern )
				.setInvariantType( returnType )
				.register();
	}

	/**
	 * Get a builder for creating and registering a pattern-based function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param pattern The pattern defining the underlying function call
	 *
	 * @return The builder
	 */
	public PatternFunctionDescriptorBuilder patternDescriptorBuilder(String registrationKey, String pattern) {
		return new PatternFunctionDescriptorBuilder( this, registrationKey, FunctionKind.NORMAL, pattern );
	}

	/**
	 * Get a builder for creating and registering a pattern-based aggregate function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param pattern The pattern defining the underlying function call
	 *
	 * @return The builder
	 */
	public PatternFunctionDescriptorBuilder patternAggregateDescriptorBuilder(String registrationKey, String pattern) {
		return new PatternFunctionDescriptorBuilder( this, registrationKey, FunctionKind.AGGREGATE, pattern );
	}

	/**
	 * Register a named descriptor by name.  Shortcut for building a descriptor via
	 * {@link #namedDescriptorBuilder} using the passed name as both the registration
	 * key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNamed(String name) {
		return namedDescriptorBuilder( name ).register();
	}

	/**
	 * Register a named descriptor by name and invariant return type.  Shortcut for building
	 * a descriptor via {@link #namedDescriptorBuilder} using the passed name as both the
	 * registration key and underlying SQL function name and accepting the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNamed(String name, BasicType returnType) {
		return namedDescriptorBuilder( name, name ).setInvariantType( returnType ).register();
	}

	/**
	 * Get a builder for creating and registering a name-based function descriptor
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedDescriptorBuilder(String name) {
		return namedDescriptorBuilder( name, name );
	}

	/**
	 * Get a builder for creating and registering a name-based aggregate function descriptor
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedAggregateDescriptorBuilder(String name) {
		return namedAggregateDescriptorBuilder( name, name );
	}

	/**
	 * Get a builder for creating and registering a name-based ordered set-aggregate function descriptor
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedOrderedSetAggregateDescriptorBuilder(String name) {
		return namedOrderedSetAggregateDescriptorBuilder( name, name );
	}

	/**
	 * Get a builder for creating and registering a name-based window function descriptor
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedWindowDescriptorBuilder(String name) {
		return namedWindowDescriptorBuilder( name, name );
	}

	/**
	 * Get a builder for creating and registering a name-based set-returning function descriptor
	 * using the passed name as both the registration key and underlying SQL
	 * function name
	 *
	 * @param name The function name (and registration key)
	 * @param typeResolver The type resolver to use
	 *
	 * @return The builder
	 */
	public NamedSetReturningFunctionDescriptorBuilder namedSetReturningDescriptorBuilder(
			String name,
			SetReturningFunctionTypeResolver typeResolver) {
		return namedSetReturningDescriptorBuilder( name, name, typeResolver );
	}

	/**
	 * Get a builder for creating and registering a name-based function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param name The underlying SQL function name to use
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedDescriptorBuilder(String registrationKey, String name) {
		return new NamedFunctionDescriptorBuilder( this, registrationKey, FunctionKind.NORMAL, name );
	}

	/**
	 * Get a builder for creating and registering a name-based aggregate function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param name The underlying SQL function name to use
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedAggregateDescriptorBuilder(String registrationKey, String name) {
		return new NamedFunctionDescriptorBuilder( this, registrationKey, FunctionKind.AGGREGATE, name );
	}

	/**
	 * Get a builder for creating and registering a name-based ordered set-aggregate function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param name The underlying SQL function name to use
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedOrderedSetAggregateDescriptorBuilder(String registrationKey, String name) {
		return new NamedFunctionDescriptorBuilder( this, registrationKey, FunctionKind.ORDERED_SET_AGGREGATE, name );
	}

	/**
	 * Get a builder for creating and registering a name-based window function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param name The underlying SQL function name to use
	 *
	 * @return The builder
	 */
	public NamedFunctionDescriptorBuilder namedWindowDescriptorBuilder(String registrationKey, String name) {
		return new NamedFunctionDescriptorBuilder( this, registrationKey, FunctionKind.WINDOW, name );
	}

	/**
	 * Get a builder for creating and registering a name-based set-returning function descriptor.
	 *
	 * @param registrationKey The name under which the descriptor will get registered
	 * @param name The underlying SQL function name to use
	 * @param typeResolver The type resolver to use
	 *
	 * @return The builder
	 */
	public NamedSetReturningFunctionDescriptorBuilder namedSetReturningDescriptorBuilder(
			String registrationKey,
			String name,
			SetReturningFunctionTypeResolver typeResolver) {
		return new NamedSetReturningFunctionDescriptorBuilder( this, registrationKey, name, typeResolver );
	}

	public NamedFunctionDescriptorBuilder noArgsBuilder(String name) {
		return noArgsBuilder( name, name );
	}

	public NamedFunctionDescriptorBuilder noArgsBuilder(String registrationKey, String name) {
		return namedDescriptorBuilder( registrationKey, name )
				.setExactArgumentCount( 0 );
	}

	/**
	 * Specialized registration method for registering a named descriptor for functions
	 * expecting zero arguments.  Short-cut for building a named descriptor via
	 * {@link #namedDescriptorBuilder} specifying zero arguments and accepting the
	 * rest of the builder's defaults.
	 *
	 * @param name The function name (and registration key)
	 */
	public SqmFunctionDescriptor registerNoArgs(String name) {
		return registerNoArgs( name, name );
	}

	public SqmFunctionDescriptor registerNoArgs(String registrationKey, String name) {
		return noArgsBuilder( registrationKey, name ).register();
	}

	public SqmFunctionDescriptor registerNoArgs(String name, BasicType returnType) {
		return registerNoArgs( name, name, returnType );
	}

	public SqmFunctionDescriptor registerNoArgs(String registrationKey, String name, BasicType returnType) {
		return noArgsBuilder( registrationKey, name )
				.setInvariantType( returnType )
				.register();
	}

	public SqmFunctionDescriptor wrapInJdbcEscape(String name, SqmFunctionDescriptor wrapped) {
		final JdbcEscapeFunctionDescriptor wrapperTemplate = new JdbcEscapeFunctionDescriptor( name, wrapped );
		register( name, wrapperTemplate );
		return wrapperTemplate;
	}

	public void registerAlternateKey(String alternateKey, String mappedKey) {
		assert functionMap.containsKey( mappedKey );
		log.debugf( "Registering alternate key : %s -> %s", alternateKey, mappedKey );
		alternateKeyMap.put( alternateKey, mappedKey );
	}

	/**
	 * Register a nullary/unary function.
	 *
	 * i.e. a function which accepts 0-1 arguments.
	 */
	public MultipatternSqmFunctionDescriptor registerNullaryUnaryPattern(
			String name,
			BasicType type,
			String pattern0,
			String pattern1,
			FunctionParameterType parameterType,
			TypeConfiguration typeConfiguration) {
		return registerPatterns(
				name,
				type,
				new FunctionParameterType[] { parameterType },
				typeConfiguration,
				pattern0,
				pattern1
		);
	}

	/**
	 * Register a unary/binary function.
	 *
	 * i.e. a function which accepts 1-2 arguments.
	 */
	public MultipatternSqmFunctionDescriptor registerUnaryBinaryPattern(
			String name,
			String pattern1,
			String pattern2,
			FunctionParameterType parameterType1,
			FunctionParameterType parameterType2,
			TypeConfiguration typeConfiguration) {
		return registerPatterns(
				name,
				new FunctionParameterType[] { parameterType1, parameterType2 },
				typeConfiguration,
				null,
				pattern1,
				pattern2
		);
	}
	/**
	 * Register a unary/binary function.
	 *
	 * i.e. a function which accepts 1-2 arguments.
	 */
	public MultipatternSqmFunctionDescriptor registerUnaryBinaryPattern(
			String name,
			BasicType<?> type,
			String pattern1,
			String pattern2,
			FunctionParameterType parameterType1,
			FunctionParameterType parameterType2,
			TypeConfiguration typeConfiguration) {
		return registerPatterns(
				name,
				type,
				new FunctionParameterType[] { parameterType1, parameterType2 },
				typeConfiguration,
				null,
				pattern1,
				pattern2
		);
	}

	/**
	 * Register a binary/ternary function.
	 *
	 * i.e. a function which accepts 2-3 arguments.
	 */
	public MultipatternSqmFunctionDescriptor registerBinaryTernaryPattern(
			String name,
			BasicType<?> type,
			String pattern2,
			String pattern3,
			FunctionParameterType parameterType1,
			FunctionParameterType parameterType2,
			FunctionParameterType parameterType3,
			TypeConfiguration typeConfiguration) {
		return registerPatterns(
				name,
				type,
				new FunctionParameterType[] { parameterType1, parameterType2, parameterType3 },
				typeConfiguration,
				null,
				null,
				pattern2,
				pattern3
		);
	}

	/**
	 * Register a ternary/quaternary function.
	 *
	 * i.e. a function which accepts 3-4 arguments.
	 */
	public MultipatternSqmFunctionDescriptor registerTernaryQuaternaryPattern(
			String name,
			BasicType<?> type,
			String pattern3,
			String pattern4,
			FunctionParameterType parameterType1,
			FunctionParameterType parameterType2,
			FunctionParameterType parameterType3,
			FunctionParameterType parameterType4,
			TypeConfiguration typeConfiguration) {
		return registerPatterns(
				name,
				type,
				new FunctionParameterType[] {
						parameterType1,
						parameterType2,
						parameterType3,
						parameterType4
				},
				typeConfiguration,
				null,
				null,
				null,
				pattern3,
				pattern4
		);
	}

	private MultipatternSqmFunctionDescriptor registerPatterns(
			String name,
			FunctionParameterType[] parameterTypes,
			TypeConfiguration typeConfiguration,
			String... patterns) {
		SqmFunctionDescriptor[] descriptors =
				new SqmFunctionDescriptor[patterns.length];
		for ( int i = 0; i < patterns.length; i++ ) {
			String pattern = patterns[i];
			if ( pattern != null ) {
				descriptors[i] =
						patternDescriptorBuilder( name, pattern )
								.setExactArgumentCount( i )
								.setParameterTypes( parameterTypes )
								.setReturnTypeResolver( useArgType(1) )
								.descriptor();
			}
		}

		MultipatternSqmFunctionDescriptor function =
				new MultipatternSqmFunctionDescriptor( name, descriptors, typeConfiguration, parameterTypes );
		register( name, function );
		return function;
	}

	private MultipatternSqmFunctionDescriptor registerPatterns(
			String name,
			BasicType<?> type,
			FunctionParameterType[] parameterTypes,
			TypeConfiguration typeConfiguration,
			String... patterns) {
		SqmFunctionDescriptor[] descriptors =
				new SqmFunctionDescriptor[patterns.length];
		for ( int i = 0; i < patterns.length; i++ ) {
			String pattern = patterns[i];
			if ( pattern != null ) {
				descriptors[i] =
						patternDescriptorBuilder( name, pattern )
								.setExactArgumentCount( i )
								.setParameterTypes( parameterTypes )
								.setInvariantType( type )
								.descriptor();
			}
		}

		MultipatternSqmFunctionDescriptor function =
				new MultipatternSqmFunctionDescriptor( name, descriptors, type, typeConfiguration, parameterTypes );
		register( name, function );
		return function;
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
