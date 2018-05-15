/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.ArgumentDomainResult;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationConstructorAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationInjectionAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationListAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationMapAssemblerImpl;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DynamicInstantiationResult;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationResultImpl implements DynamicInstantiationResult {
	private static final Logger log = Logger.getLogger( DynamicInstantiationResultImpl.class );

	private final String resultVariable;

	private final DynamicInstantiationNature nature;
	private final JavaTypeDescriptor<Object> javaTypeDescriptor;
	private final List<ArgumentDomainResult> argumentResults;

	public DynamicInstantiationResultImpl(
			String resultVariable,
			DynamicInstantiationNature nature,
			JavaTypeDescriptor<Object> javaTypeDescriptor,
			List<ArgumentDomainResult> argumentResults) {
		this.resultVariable = resultVariable;
		this.nature = nature;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.argumentResults = argumentResults;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {

		boolean areAllArgumentsAliased = true;
		boolean areAnyArgumentsAliased = false;
		final Set<String> aliases = new HashSet<>();
		final List<String> duplicatedAliases = new ArrayList<>();
		final List<ArgumentReader> argumentReaders = new ArrayList<>();

		if ( argumentResults != null ) {
			for ( ArgumentDomainResult argumentResult : argumentResults ) {
				final String argumentAlias = argumentResult.getResultVariable();
				if ( argumentAlias == null ) {
					areAllArgumentsAliased = false;
				}
				else {
					if ( !aliases.add( argumentAlias ) ) {
						duplicatedAliases.add( argumentAlias );
						log.debugf( "Query defined duplicate resultVariable encountered multiple declarations of [%s]",
									argumentAlias
						);
					}
					areAnyArgumentsAliased = true;
				}

				argumentReaders.add(
						argumentResult.createResultAssembler( initializerConsumer, creationOptions, creationContext )
				);
			}
		}

		final DomainResultAssembler assembler = resolveAssembler(
				areAllArgumentsAliased,
				areAnyArgumentsAliased,
				duplicatedAliases,
				argumentReaders,
				creationContext
		);

		return assembler;
	}

	@SuppressWarnings("unchecked")
	private DomainResultAssembler resolveAssembler(
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader> argumentReaders,
			AssemblerCreationContext creationContext) {

		if ( nature == DynamicInstantiationNature.LIST ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new DynamicInstantiationListAssemblerImpl(
					(BasicJavaDescriptor) javaTypeDescriptor,
					argumentReaders
			);
		}
		else if ( nature == DynamicInstantiationNature.MAP ) {
			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException( "Map dynamic instantiation contained one or more arguments with no alias" );
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Map dynamic instantiation contained arguments with duplicated aliases [" + StringHelper.join( ",", duplicatedAliases ) + "]"
				);
			}
			return new DynamicInstantiationMapAssemblerImpl(
					(BasicJavaDescriptor) javaTypeDescriptor,
					argumentReaders
			);
		}
		else {
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor constructor : javaTypeDescriptor.getJavaType().getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != argumentReaders.size() ) {
					continue;
				}

				for ( int i = 0; i < argumentReaders.size(); i++ ) {
					final ArgumentReader argumentReader = argumentReaders.get( i );
					final JavaTypeDescriptor argumentTypeDescriptor = creationContext.getSessionFactory()
							.getTypeConfiguration()
							.getJavaTypeDescriptorRegistry()
							.getOrMakeJavaDescriptor( constructor.getParameterTypes()[i] );

					final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
							argumentTypeDescriptor,
							argumentReader.getJavaTypeDescriptor()
					);
					if ( !assignmentCompatible ) {
						log.debugf(
								"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
								i,
								constructor.getParameterTypes()[i].getName(),
								argumentTypeDescriptor.getJavaType().getName()
						);
						continue constructor_loop;
					}
				}

				constructor.setAccessible( true );
				return new DynamicInstantiationConstructorAssemblerImpl(
						constructor,
						javaTypeDescriptor,
						argumentReaders
				);
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					javaTypeDescriptor.getJavaType().getName()
			);


			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException(
						"Could not determine appropriate instantiation strategy - no matching constructor found and one or more arguments did not define alias for bean-injection"
				);
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Could not determine appropriate instantiation strategy - no matching constructor found and arguments defined duplicated aliases [" +
								StringHelper.join( ",", duplicatedAliases ) + "] for bean-injection"
				);
			}

			return new DynamicInstantiationInjectionAssemblerImpl( javaTypeDescriptor, argumentReaders );
		}
	}
}
