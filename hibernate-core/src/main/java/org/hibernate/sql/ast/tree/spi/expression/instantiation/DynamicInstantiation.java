/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.results.internal.domain.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationConstructorAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationInjectionAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationListAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationMapAssemblerImpl;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Represents a dynamic-instantiation (from an SQM query) as a QueryResultProducer (Selectable)
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements DomainResultProducer {
	private static final Logger log = Logger.getLogger( DynamicInstantiation.class );

	private final DynamicInstantiationNature nature;
	private final JavaTypeDescriptor<T> targetJavaTypeDescriptor;
	private List<DynamicInstantiationArgument> arguments;

	private boolean argumentAdditionsComplete = false;

	public DynamicInstantiation(
			DynamicInstantiationNature nature,
			JavaTypeDescriptor<T> targetJavaTypeDescriptor) {
		this.nature = nature;
		this.targetJavaTypeDescriptor = targetJavaTypeDescriptor;
	}

	public DynamicInstantiationNature getNature() {
		return nature;
	}

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

		if ( List.class.equals( getTargetJavaType() ) ) {
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
		else if ( Map.class.equals( getTargetJavaType() ) ) {
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
		return "DynamicInstantiation(" + getTargetJavaType().getName() + ")";
	}

	@Override
	@SuppressWarnings("unchecked")
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new DynamicInstantiationResultImpl(
				resultVariable,
				getNature(),
				(JavaTypeDescriptor) getTargetJavaTypeDescriptor(),
				getArguments().stream()
						.map( argument -> argument.buildArgumentDomainResult( creationContext, creationState ) )
						.collect( Collectors.toList() )
		);
	}

	@SuppressWarnings("unchecked")
	private static DomainResultAssembler resolveAssembler(
			DynamicInstantiation dynamicInstantiation,
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader> argumentReaders,
			AssemblerCreationContext creationContext) {

		if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.LIST ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new DynamicInstantiationListAssemblerImpl(
					(BasicJavaDescriptor<List>) dynamicInstantiation.getTargetJavaTypeDescriptor(),
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
					(BasicJavaDescriptor<Map>) dynamicInstantiation.getTargetJavaTypeDescriptor(),
					argumentReaders
			);
		}
		else {
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor constructor : dynamicInstantiation.getTargetJavaType().getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != dynamicInstantiation.arguments.size() ) {
					continue;
				}

				for ( int i = 0; i < dynamicInstantiation.arguments.size(); i++ ) {
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
						dynamicInstantiation.getTargetJavaTypeDescriptor(),
						argumentReaders
				);
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					dynamicInstantiation.getTargetJavaType().getName()
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

			return new DynamicInstantiationInjectionAssemblerImpl( dynamicInstantiation.getTargetJavaTypeDescriptor(), argumentReaders );
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
