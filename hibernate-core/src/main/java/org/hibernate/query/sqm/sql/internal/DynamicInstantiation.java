/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.sqm.sql.ConversionException;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Represents a dynamic-instantiation (from an SQM query) as a DomainResultProducer
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements DomainResultProducer {
	private static final Logger log = Logger.getLogger( DynamicInstantiation.class );

	private final DynamicInstantiationNature nature;
	private final JavaTypeDescriptor<T> targetJavaTypeDescriptor;
	private List<DynamicInstantiationArgument> arguments;

	private boolean argumentAdditionsComplete = false;

	@SuppressWarnings("WeakerAccess")
	public DynamicInstantiation(
			DynamicInstantiationNature nature,
			JavaTypeDescriptor<T> targetJavaTypeDescriptor) {
		this.nature = nature;
		this.targetJavaTypeDescriptor = targetJavaTypeDescriptor;
	}

	public DynamicInstantiationNature getNature() {
		return nature;
	}

	@SuppressWarnings("WeakerAccess")
	public JavaTypeDescriptor<T> getTargetJavaTypeDescriptor() {
		return targetJavaTypeDescriptor;
	}

	/**
	 * todo (6.0) : remove this.  find usages and replace with #getTargetJavaTypeDescriptor
	 */
	public Class<T> getTargetJavaType() {
		return getTargetJavaTypeDescriptor().getJavaType();
	}

	public void addArgument(String alias, DomainResultProducer argumentResultProducer) {
		if ( argumentAdditionsComplete ) {
			throw new ConversionException( "Unexpected call to DynamicInstantiation#addAgument after previously complete" );
		}

		if ( List.class.equals( getTargetJavaTypeDescriptor().getJavaType() ) ) {
			// really should not have an alias...
			if ( alias != null ) {
				log.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						argumentResultProducer.toString(),
						alias
				);
			}
		}
		else if ( Map.class.equals( getTargetJavaTypeDescriptor().getJavaType() ) ) {
			// must have an alias...
			if ( alias == null ) {
				log.warnf(
						"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias', " +
								"but such aliases are needed for dynamic Map instantiations; " +
								"will likely cause problems later processing query results",
						argumentResultProducer.toString()
				);
			}
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}

		arguments.add( new DynamicInstantiationArgument( argumentResultProducer, alias ) );
	}

	public void complete() {
		// called after all arguments have been registered...
		argumentAdditionsComplete = true;
	}

	public List<DynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return "DynamicInstantiation(" + getTargetJavaTypeDescriptor().getJavaType().getName() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState) {
//		return new DynamicInstantiationResult(
//				resultVariable,
//				getNature(),
//				(JavaTypeDescriptor) getTargetJavaTypeDescriptor(),
//				getArguments().stream()
//						.map( argument -> argument.buildArgumentDomainResult( creationState ) )
//						.collect( Collectors.toList() )
//		);
//	}
//
	@SuppressWarnings("unchecked")
	private static DomainResultAssembler resolveAssembler(
			DynamicInstantiation dynamicInstantiation,
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader<?>> argumentReaders,
			AssemblerCreationState creationState) {

		if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.LIST ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new DynamicInstantiationListAssemblerImpl(
					(JavaTypeDescriptor<List>) dynamicInstantiation.getTargetJavaTypeDescriptor(),
					argumentReaders
			);
		}
		else if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.MAP ) {
			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException( "Map dynamic instantiation contained one or more arguments with no alias" );
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Map dynamic instantiation contained arguments with duplicated aliases [" + StringHelper.join( ",", duplicatedAliases ) + "]"
				);
			}
			return new DynamicInstantiationMapAssemblerImpl(
					(JavaTypeDescriptor<Map>) dynamicInstantiation.getTargetJavaTypeDescriptor(),
					argumentReaders
			);
		}
		else {
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor constructor : dynamicInstantiation.getTargetJavaTypeDescriptor().getJavaType().getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != dynamicInstantiation.arguments.size() ) {
					continue;
				}

				for ( int i = 0; i < dynamicInstantiation.arguments.size(); i++ ) {
					final ArgumentReader argumentReader = argumentReaders.get( i );
					final JavaTypeDescriptor argumentTypeDescriptor = creationState.getSqlAstCreationContext().getDomainModel()
							.getTypeConfiguration()
							.getJavaTypeDescriptorRegistry()
							.resolveDescriptor( constructor.getParameterTypes()[i] );

					final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
							argumentTypeDescriptor,
							argumentReader.getAssembledJavaTypeDescriptor()
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
						dynamicInstantiation.getTargetJavaTypeDescriptor(),
						argumentReaders
				);
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					dynamicInstantiation.getTargetJavaTypeDescriptor().getJavaType().getName()
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

			return new DynamicInstantiationInjectionAssemblerImpl(
					dynamicInstantiation.getTargetJavaTypeDescriptor(),
					argumentReaders
			);
		}
	}

	class Builder {
		private final DynamicInstantiationNature nature;
		private final JavaTypeDescriptor<T> targetJavaTypeDescriptor;
		private List<DynamicInstantiationArgument> arguments;

		public Builder(
				DynamicInstantiationNature nature,
				JavaTypeDescriptor<T> targetJavaTypeDescriptor) {
			this.nature = nature;
			this.targetJavaTypeDescriptor = targetJavaTypeDescriptor;
		}


	}
}
