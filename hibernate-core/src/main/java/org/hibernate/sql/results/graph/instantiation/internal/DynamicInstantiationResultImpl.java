/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.instantiation.DynamicInstantiationResult;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.jboss.logging.Logger;

import static java.util.stream.Collectors.toList;
import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.findMatchingConstructor;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationResultImpl<R> implements DynamicInstantiationResult<R> {
	private static final Logger LOG = Logger.getLogger( DynamicInstantiationResultImpl.class );

	private final String resultVariable;

	private final DynamicInstantiationNature nature;
	private final JavaType<R> javaType;
	private final List<ArgumentDomainResult<?>> argumentResults;

	public DynamicInstantiationResultImpl(
			String resultVariable,
			DynamicInstantiationNature nature,
			JavaType<R> javaType,
			List<ArgumentDomainResult<?>> argumentResults) {
		this.resultVariable = resultVariable;
		this.nature = nature;
		this.javaType = javaType;
		this.argumentResults = argumentResults;
	}

	@Override
	public JavaType<R> getResultJavaType() {
		return javaType;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < argumentResults.size(); i++ ) {
			final var argumentResult = argumentResults.get( i );
			if ( argumentResult.containsAnyNonScalarResults() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		for ( var argumentResult : argumentResults ) {
			argumentResult.collectValueIndexesToCache( valueIndexes );
		}
	}

	@Override
	public DomainResultAssembler<R> createResultAssembler(InitializerParent<?> parent, AssemblerCreationState creationState) {
		boolean areAllArgumentsAliased = true;
		boolean areAnyArgumentsAliased = false;
		final Set<String> aliases = new HashSet<>();
		final List<String> duplicatedAliases = new ArrayList<>();
		final List<ArgumentReader<?>> argumentReaders = new ArrayList<>();

		if ( argumentResults != null ) {
			for ( var argumentResult : argumentResults ) {
				final String argumentAlias = argumentResult.getResultVariable();
				if ( argumentAlias == null ) {
					areAllArgumentsAliased = false;
				}
				else {
					if ( !aliases.add( argumentAlias ) ) {
						duplicatedAliases.add( argumentAlias );
						LOG.debugf(
								"Query defined duplicate resultVariable encountered multiple declarations of [%s]",
								argumentAlias
						);
					}
					areAnyArgumentsAliased = true;
				}

				argumentReaders.add( argumentResult.createResultAssembler( parent, creationState ) );
			}
		}

		return resolveAssembler(
				areAllArgumentsAliased,
				areAnyArgumentsAliased,
				duplicatedAliases,
				argumentReaders,
				creationState
		);
	}

	@SuppressWarnings("unchecked")
	private DomainResultAssembler<R> resolveAssembler(
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader<?>> argumentReaders,
			AssemblerCreationState creationState) {

		if ( nature == DynamicInstantiationNature.LIST ) {
			if ( LOG.isDebugEnabled() && areAnyArgumentsAliased ) {
				LOG.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return (DomainResultAssembler<R>)
					new DynamicInstantiationAssemblerListImpl( (JavaType<List<?>>) javaType, argumentReaders );
		}
		else if ( nature == DynamicInstantiationNature.MAP ) {
			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException( "Map instantiation contained one or more arguments with no alias" );
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Map instantiation has arguments with duplicate aliases ["
								+ StringHelper.join( ",", duplicatedAliases ) + "]"
				);
			}
			return (DomainResultAssembler<R>)
					new DynamicInstantiationAssemblerMapImpl( (JavaType<Map<?,?>>) javaType, argumentReaders );
		}
		else {
			return assembler( areAllArgumentsAliased, duplicatedAliases, argumentReaders, creationState );
		}
	}

	private DomainResultAssembler<R> assembler(
			boolean areAllArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader<?>> argumentReaders,
			AssemblerCreationState creationState) {
		// find a constructor matching argument types
		final var constructor = findMatchingConstructor(
				javaType.getJavaTypeClass(),
				argumentReaders.stream()
						.map( reader -> argumentClass( reader ) )
						.collect( toList() ),
				creationState.getSqlAstCreationContext()
						.getMappingMetamodel()
						.getTypeConfiguration()
		);
		if ( constructor != null ) {
			constructor.setAccessible( true );
			return new DynamicInstantiationAssemblerConstructorImpl<>( constructor, javaType, argumentReaders );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					javaType.getTypeName()
			);
		}

		if ( !areAllArgumentsAliased) {
			throw new IllegalStateException(
					"Cannot instantiate class '" + javaType.getTypeName() + "'"
							+ " (it has no constructor with signature " + signature()
							+ ", and not every argument has an alias)"
			);
		}
		if ( !duplicatedAliases.isEmpty() ) {
			throw new IllegalStateException(
					"Cannot instantiate class '" + javaType.getTypeName() + "'"
							+ " (it has no constructor with signature " + signature()
							+ ", and has arguments with duplicate aliases ["
							+ StringHelper.join( ",", duplicatedAliases) + "])"
			);
		}

		return new DynamicInstantiationAssemblerInjectionImpl<>( javaType, argumentReaders );
	}

	private static Class<?> argumentClass(ArgumentReader<?> reader) {
		final var assembledJavaType = reader.getAssembledJavaType();
		return assembledJavaType instanceof DateJavaType temporalJavaType
				// Hack to accommodate a constructor with java.sql parameter
				// types when the entity has java.util.Date as its field types.
				// (This was requested in HHH-4179 and we fixed it by accident.)
				? TemporalJavaType.resolveJavaTypeClass( temporalJavaType.getPrecision() )
				: assembledJavaType.getJavaTypeClass();
	}

	private List<String> signature() {
		return argumentResults.stream()
				.map( adt -> adt.getResultJavaType().getTypeName() )
				.collect( toList() );
	}
}
