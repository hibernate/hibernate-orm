/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.query.sqm.sql.ConversionException;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * Represents a dynamic-instantiation (from an SQM query) as a DomainResultProducer
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements DomainResultProducer<T> {
	private static final Logger LOG = Logger.getLogger( DynamicInstantiation.class );

	private final DynamicInstantiationNature nature;
	private final JavaType<T> targetJavaType;
	private List<DynamicInstantiationArgument<?>> arguments;

	private boolean argumentAdditionsComplete = false;

	public DynamicInstantiation(
			DynamicInstantiationNature nature,
			JavaType<T> targetJavaType) {
		this.nature = nature;
		this.targetJavaType = targetJavaType;
	}

	public DynamicInstantiationNature getNature() {
		return nature;
	}

	public JavaType<T> getTargetJavaType() {
		return targetJavaType;
	}

	public void addArgument(String alias, DomainResultProducer<?> argumentResultProducer) {
		if ( argumentAdditionsComplete ) {
			throw new ConversionException( "Unexpected call to DynamicInstantiation#addAgument after previously complete" );
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}

		if ( List.class.equals( getTargetJavaType().getJavaTypeClass() ) ) {
			// really should not have an alias...
			if ( alias != null && LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						argumentResultProducer.toString(),
						alias
				);
			}
		}
		else if ( Map.class.equals( getTargetJavaType().getJavaTypeClass() ) ) {
			// Retain the default alias we also used in 5.x which is the position
			if ( alias == null ) {
				alias = Integer.toString( arguments.size() );
			}
		}

		arguments.add( new DynamicInstantiationArgument<>( argumentResultProducer, alias ) );
	}

	public void complete() {
		// called after all arguments have been registered...
		argumentAdditionsComplete = true;
	}

	public List<DynamicInstantiationArgument<?>> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return "DynamicInstantiation(" + getTargetJavaType().getTypeName() + ")";
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return new DynamicInstantiationResultImpl<>(
				resultVariable,
				getNature(),
				getTargetJavaType(),
				getArguments().stream()
						.map( argument -> argument.buildArgumentDomainResult( creationState ) )
						.collect( toList() )
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "dynamic instantiation in a sub-query is unsupported" );
	}

//
//	@SuppressWarnings("unchecked")
//	private static DomainResultAssembler resolveAssembler(
//			DynamicInstantiation dynamicInstantiation,
//			boolean areAllArgumentsAliased,
//			boolean areAnyArgumentsAliased,
//			List<String> duplicatedAliases,
//			List<ArgumentReader<?>> argumentReaders,
//			AssemblerCreationState creationState) {
//
//		if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.LIST ) {
//			if ( LOG.isDebugEnabled() && areAnyArgumentsAliased ) {
//				LOG.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
//			}
//			return new DynamicInstantiationListAssemblerImpl(
//					(JavaType<List>) dynamicInstantiation.getTargetJavaType(),
//					argumentReaders
//			);
//		}
//		else if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.MAP ) {
//			if ( ! areAllArgumentsAliased ) {
//				throw new IllegalStateException( "Map dynamic instantiation contained one or more arguments with no alias" );
//			}
//			if ( !duplicatedAliases.isEmpty() ) {
//				throw new IllegalStateException(
//						"Map dynamic instantiation contained arguments with duplicated aliases [" + StringHelper.join( ",", duplicatedAliases ) + "]"
//				);
//			}
//			return new DynamicInstantiationMapAssemblerImpl(
//					(JavaType<Map>) dynamicInstantiation.getTargetJavaType(),
//					argumentReaders
//			);
//		}
//		else {
//			// find a constructor matching argument types
//			constructor_loop:
//			for ( Constructor constructor : dynamicInstantiation.getTargetJavaType().getJavaType().getDeclaredConstructors() ) {
//				if ( constructor.getParameterTypes().length != dynamicInstantiation.arguments.size() ) {
//					continue;
//				}
//
//				for ( int i = 0; i < dynamicInstantiation.arguments.size(); i++ ) {
//					final ArgumentReader argumentReader = argumentReaders.get( i );
//					final JavaType argumentType = creationState.getSqlAstCreationContext().getDomainModel()
//							.getTypeConfiguration()
//							.getJavaTypeRegistry()
//							.resolveDescriptor( constructor.getParameterTypes()[i] );
//
//					final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
//							argumentType,
//							argumentReader.getAssembledJavaType()
//					);
//					if ( !assignmentCompatible ) {
//						LOG.debugf(
//								"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
//								i,
//								constructor.getParameterTypes()[i].getName(),
//								argumentType.getJavaType().getName()
//						);
//						continue constructor_loop;
//					}
//				}
//
//				constructor.setAccessible( true );
//				return new DynamicInstantiationConstructorAssemblerImpl(
//						constructor,
//						dynamicInstantiation.getTargetJavaType(),
//						argumentReaders
//				);
//			}
//
//			LOG.debugf(
//					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
//					dynamicInstantiation.getTargetJavaType().getJavaType().getName()
//			);
//
//
//			if ( ! areAllArgumentsAliased ) {
//				throw new IllegalStateException(
//						"Could not determine appropriate instantiation strategy - no matching constructor found and one or more arguments did not define alias for bean-injection"
//				);
//			}
//			if ( !duplicatedAliases.isEmpty() ) {
//				throw new IllegalStateException(
//						"Could not determine appropriate instantiation strategy - no matching constructor found and arguments defined duplicated aliases [" +
//								StringHelper.join( ",", duplicatedAliases ) + "] for bean-injection"
//				);
//			}
//
//			return new DynamicInstantiationInjectionAssemblerImpl(
//					dynamicInstantiation.getTargetJavaType(),
//					argumentReaders
//			);
//		}
//	}
//
//	class Builder {
//		private final DynamicInstantiationNature nature;
//		private final JavaType<T> targetJavaType;
//		private List<DynamicInstantiationArgument> arguments;
//
//		public Builder(
//				DynamicInstantiationNature nature,
//				JavaTypeDescriptor<T> targetJavaType) {
//			this.nature = nature;
//			this.targetJavaType = targetJavaType;
//		}
//
//
//	}
}
