/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.results.internal.DynamicInstantiationQueryResultImpl;
import org.hibernate.sql.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationConstructorAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationInjectionAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationListAssemblerImpl;
import org.hibernate.sql.results.internal.instantiation.DynamicInstantiationMapAssemblerImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.CLASS;
import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.LIST;
import static org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature.MAP;

/**
 * Represents a dynamic instantiation ({@code select new XYZ(...) ...}) as part of the SQM.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiation
		implements SqmSelectableNode, SqmAliasedExpressionContainer<SqmDynamicInstantiationArgument> {

	private static final Logger log = Logger.getLogger( SqmDynamicInstantiation.class );

	public static SqmDynamicInstantiation forClassInstantiation(JavaTypeDescriptor targetJavaType) {
		return new SqmDynamicInstantiation(
				new DynamicInstantiationTargetImpl( CLASS, targetJavaType )
		);
	}

	public static SqmDynamicInstantiation forMapInstantiation(JavaTypeDescriptor<Map> mapJavaTypeDescriptor) {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( MAP, mapJavaTypeDescriptor ) );
	}

	public static SqmDynamicInstantiation forListInstantiation(JavaTypeDescriptor<List> listJavaTypeDescriptor) {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( LIST, listJavaTypeDescriptor ) );
	}

	private final SqmDynamicInstantiationTarget instantiationTarget;
	private List<SqmDynamicInstantiationArgument> arguments;

	private SqmDynamicInstantiation(SqmDynamicInstantiationTarget instantiationTarget) {
		this.instantiationTarget = instantiationTarget;
	}

	public SqmDynamicInstantiationTarget getInstantiationTarget() {
		return instantiationTarget;
	}

	public List<SqmDynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	@Override
	public JavaTypeDescriptor getProducedJavaTypeDescriptor() {
		return getInstantiationTarget().getTargetTypeDescriptor();
	}

	@Override
	public String asLoggableText() {
		return "<new " + instantiationTarget.getJavaType().getName() + ">";
	}

	public void addArgument(SqmDynamicInstantiationArgument argument) {
		if ( instantiationTarget.getNature() == LIST ) {
			// really should not have an alias...
			if ( argument.getAlias() != null ) {
				log.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						argument.getSelectableNode().asLoggableText(),
						argument.getAlias()
				);
			}
		}
		else if ( instantiationTarget.getNature() == MAP ) {
			// must(?) have an alias...
			log.warnf(
					"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
							"but such aliases are needed for dynamic Map instantiations; " +
							"will likely cause problems later translating sqm",
					argument.getSelectableNode().asLoggableText(),
					argument.getAlias()
			);
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( argument );
	}

	@Override
	public SqmDynamicInstantiationArgument add(SqmExpression expression, String alias) {
		SqmDynamicInstantiationArgument argument = new SqmDynamicInstantiationArgument( expression, alias );
		addArgument( argument );
		return argument;
	}

	@Override
	public void add(SqmDynamicInstantiationArgument aliasExpression) {
		addArgument( aliasExpression );
	}

	@Override
	public QueryResult createQueryResult(
			SemanticQueryWalker walker,
			String resultVariable,
			QueryResultCreationContext creationContext) {

		boolean areAllArgumentsAliased = true;
		boolean areAnyArgumentsAliased = false;
		final Set<String> aliases = new HashSet<>();
		final List<String> duplicatedAliases = new ArrayList<>();

		final DynamicInstantiationNature instantiationNature = getInstantiationTarget().getNature();
		final JavaTypeDescriptor<Object> targetTypeDescriptor = interpretInstantiationTarget(
				getInstantiationTarget(),
				creationContext
		);

		final List<ArgumentReader> argumentReaders = new ArrayList<>();
		for ( SqmDynamicInstantiationArgument argument : getArguments() ) {
			final SqmSelectableNode selectableNode = argument.getSelectableNode();
//			final QueryResultProducer argumentResultProducer = argument.getQueryResultProducer();

			if ( argument.getAlias() == null ) {
				areAllArgumentsAliased = false;

				if ( instantiationNature == MAP ) {
					log.warnf(
							"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias', " +
									"but such aliases are needed for dynamic Map instantiations; " +
									"will likely cause problems later processing query results",
							selectableNode.asLoggableText()
					);
				}
			}
			else {
				if ( !aliases.add( argument.getAlias() ) ) {
					duplicatedAliases.add( argument.getAlias() );
					log.debugf( "Query defined duplicate resultVariable encountered multiple declarations of [%s]", argument.getAlias() );
				}
				areAnyArgumentsAliased = true;

				if ( instantiationNature == LIST ) {
					log.debugf(
							"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
									"but such aliases are ignored for dynamic List instantiations",
							selectableNode.asLoggableText(),
							argument.getAlias()
					);
				}
			}

			final QueryResult queryResult = selectableNode.createQueryResult(
					walker,
					argument.getAlias(),
					creationContext
			);

			argumentReaders.add(
					new ArgumentReader(
							queryResult.getResultAssembler(),
							argument.getAlias()
					)
			);
		}

		final QueryResultAssembler assembler = resolveAssembler(
				instantiationNature,
				targetTypeDescriptor,
				areAllArgumentsAliased,
				areAnyArgumentsAliased,
				duplicatedAliases,
				argumentReaders,
				creationContext
		);

		return new DynamicInstantiationQueryResultImpl( resultVariable, assembler );
	}

	@SuppressWarnings("unchecked")
	private <T> JavaTypeDescriptor<T> interpretInstantiationTarget(
			SqmDynamicInstantiationTarget instantiationTarget,
			QueryResultCreationContext creationContext) {
		final Class<T> targetJavaType;

		if ( instantiationTarget.getNature() == DynamicInstantiationNature.LIST ) {
			targetJavaType = (Class<T>) List.class;
		}
		else if ( instantiationTarget.getNature() == DynamicInstantiationNature.MAP ) {
			targetJavaType = (Class<T>) Map.class;
		}
		else {
			targetJavaType = instantiationTarget.getJavaType();
		}

		return creationContext.getSessionFactory()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( targetJavaType );
	}

	@SuppressWarnings("unchecked")
	private static QueryResultAssembler resolveAssembler(
			DynamicInstantiationNature nature,
			JavaTypeDescriptor targetJavaTypeDescriptor,
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader> argumentReaders,
			QueryResultCreationContext creationContext) {

		if ( nature == DynamicInstantiationNature.LIST ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new DynamicInstantiationListAssemblerImpl(
					(BasicJavaDescriptor<List>) targetJavaTypeDescriptor,
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
					(BasicJavaDescriptor<Map>) targetJavaTypeDescriptor,
					argumentReaders
			);
		}
		else {
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor constructor : targetJavaTypeDescriptor.getJavaType().getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != argumentReaders.size() ) {
					continue;
				}

				for ( int i = 0; i < argumentReaders.size(); i++ ) {
					final ArgumentReader argumentReader = argumentReaders.get( i );
					final JavaTypeDescriptor argumentTypeDescriptor = creationContext.getSessionFactory()
							.getTypeConfiguration()
							.getJavaTypeDescriptorRegistry()
							.getDescriptor( constructor.getParameterTypes()[i] );

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
						targetJavaTypeDescriptor,
						argumentReaders
				);
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					targetJavaTypeDescriptor.getTypeName()
			);


			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException( "Bean-injection dynamic instantiation contained one or more arguments with no alias" );
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Bean-injection dynamic instantiation contained arguments with duplicated aliases [" + StringHelper
								.join( ",", duplicatedAliases ) + "]"
				);
			}

			return new DynamicInstantiationInjectionAssemblerImpl( targetJavaTypeDescriptor, argumentReaders );
		}
	}

	public SqmDynamicInstantiation makeShallowCopy() {
		return new SqmDynamicInstantiation( getInstantiationTarget() );
	}

	private static class DynamicInstantiationTargetImpl implements SqmDynamicInstantiationTarget {
		private final DynamicInstantiationNature nature;
		private final JavaTypeDescriptor javaTypeDescriptor;


		public DynamicInstantiationTargetImpl(DynamicInstantiationNature nature, JavaTypeDescriptor javaTypeDescriptor) {
			this.nature = nature;
			this.javaTypeDescriptor = javaTypeDescriptor;
		}

		@Override
		public DynamicInstantiationNature getNature() {
			return nature;
		}

		@Override
		public JavaTypeDescriptor getTargetTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}
}
