/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.instantiation.DynamicInstantiationResult;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import static java.util.stream.Collectors.toList;
import static org.hibernate.query.sqm.tree.expression.Compatibility.areAssignmentCompatible;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationResultImpl<R> implements DynamicInstantiationResult<R> {
	private static final Logger log = Logger.getLogger( DynamicInstantiationResultImpl.class );

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
			final ArgumentDomainResult<?> argumentResult = argumentResults.get( i );
			if ( argumentResult.containsAnyNonScalarResults() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public DomainResultAssembler<R> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		boolean areAllArgumentsAliased = true;
		boolean areAnyArgumentsAliased = false;
		final Set<String> aliases = new HashSet<>();
		final List<String> duplicatedAliases = new ArrayList<>();
		final List<ArgumentReader<?>> argumentReaders = new ArrayList<>();

		if ( argumentResults != null ) {
			for ( ArgumentDomainResult<?> argumentResult : argumentResults ) {
				final String argumentAlias = argumentResult.getResultVariable();
				if ( argumentAlias == null ) {
					areAllArgumentsAliased = false;
				}
				else {
					if ( !aliases.add( argumentAlias ) ) {
						duplicatedAliases.add( argumentAlias );
						log.debugf(
								"Query defined duplicate resultVariable encountered multiple declarations of [%s]",
								argumentAlias
						);
					}
					areAnyArgumentsAliased = true;
				}

				argumentReaders.add( argumentResult.createResultAssembler( parentAccess, creationState ) );
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
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
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
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor<?> constructor : javaType.getJavaTypeClass().getDeclaredConstructors() ) {
				final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
				if ( genericParameterTypes.length == argumentReaders.size() ) {
					for ( int i = 0; i < argumentReaders.size(); i++ ) {
						final ArgumentReader<?> argumentReader = argumentReaders.get( i );
						final JavaType<?> argumentTypeDescriptor = creationState.getSqlAstCreationContext()
								.getMappingMetamodel()
								.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( genericParameterTypes[i] );

						if ( !areAssignmentCompatible( argumentTypeDescriptor, argumentReader.getAssembledJavaType() ) ) {
							if ( log.isDebugEnabled() ) {
								log.debugf(
										"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
										i,
										constructor.getParameterTypes()[i].getName(),
										argumentTypeDescriptor.getJavaType().getTypeName()
								);
							}
							continue constructor_loop;
						}
					}

					constructor.setAccessible( true );
					//noinspection rawtypes
					return new DynamicInstantiationAssemblerConstructorImpl( constructor, javaType, argumentReaders );
				}
			}

			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
						javaType.getJavaType().getTypeName()
				);
			}

			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException(
						"Cannot instantiate class '" + javaType.getJavaType().getTypeName() + "'"
								+ " (it has no constructor with signature " + signature()
								+ ", and not every argument has an alias)"
				);
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Cannot instantiate class '" + javaType.getJavaType().getTypeName() + "'"
								+ " (it has no constructor with signature " + signature()
								+ ", and has arguments with duplicate aliases ["
								+ StringHelper.join( ",", duplicatedAliases ) + "])"
				);
			}

			return new DynamicInstantiationAssemblerInjectionImpl<>( javaType, argumentReaders );
		}
	}

	private List<String> signature() {
		return argumentResults.stream()
				.map( adt -> adt.getResultJavaType().getJavaType().getTypeName() )
				.collect( toList() );
	}
}
