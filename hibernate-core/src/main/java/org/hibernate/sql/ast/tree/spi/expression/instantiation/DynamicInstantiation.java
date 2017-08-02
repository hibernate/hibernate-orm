/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.internal.NonNavigableSelectionSupport;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.exec.results.internal.QueryResultDynamicInstantiationImpl;
import org.hibernate.sql.exec.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.exec.results.internal.instantiation.DynamicInstantiationListAssemblerImpl;
import org.hibernate.sql.exec.results.internal.instantiation.DynamicInstantiationMapAssemblerImpl;
import org.hibernate.sql.exec.results.internal.instantiation.DynamicInstantiationConstructorAssemblerImpl;
import org.hibernate.sql.exec.results.internal.instantiation.DynamicInstantiationInjectionAssemblerImpl;
import org.hibernate.sql.exec.results.spi.QueryResult;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;
import org.hibernate.sql.exec.results.spi.SqlSelectionResolver;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * The Expression form of a dynamic-instantiation query node
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements Expression, Selectable {
	private static final Logger log = Logger.getLogger( DynamicInstantiation.class );

	private final DynamicInstantiationNature nature;
	private final DynamicInstantiationExpressableType<T> targetTypeDescriptor;
	private List<DynamicInstantiationArgument> arguments;

	private boolean argumentAdditionsComplete = false;

	public DynamicInstantiation(
			DynamicInstantiationNature nature,
			JavaTypeDescriptor<T> targetJavaTypeDescriptor) {
		this.nature = nature;
		this.targetTypeDescriptor = new DynamicInstantiationExpressableType<>( targetJavaTypeDescriptor );
	}

	public DynamicInstantiationNature getNature() {
		return nature;
	}

	public Class<T> getTargetJavaType() {
		return targetTypeDescriptor.getJavaType();
	}

	public void addArgument(String alias, Expression expression) {
		if ( argumentAdditionsComplete ) {
			throw new ConversionException( "Unexpected call to DynamicInstantiation#addAgument after previously complete" );
		}

		if ( List.class.equals( getTargetJavaType() ) ) {
			// really should not have an alias...
			if ( alias != null ) {
				log.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						expression.toString(),
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
						expression.toString()
				);
			}
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( new DynamicInstantiationArgument( expression, alias ) );
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
	public ExpressableType getType() {
		return targetTypeDescriptor;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitDynamicInstantiation( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression == this;

		return new DynamicInstantiationSelection(
				this,
				resultVariable
		);
	}

	/**
	 * Selection implementation for dynamic-instantiation results.
	 */
	private static class DynamicInstantiationSelection extends NonNavigableSelectionSupport {
		private final DynamicInstantiationQueryResultGenerator queryResultGenerator;

		public DynamicInstantiationSelection(
				DynamicInstantiation dynamicInstantiation,
				String resultVariable) {
			super( dynamicInstantiation, resultVariable );

			this.queryResultGenerator = new DynamicInstantiationQueryResultGenerator( this );
		}

		@Override
		public DynamicInstantiation getSelectedExpression() {
			return (DynamicInstantiation) super.getSelectedExpression();
		}

		@Override
		protected QueryResultGenerator getQueryResultGenerator() {
			return queryResultGenerator;
		}

		@Override
		public void accept(SqlAstWalker sqlTreeWalker) {
			sqlTreeWalker.visitSelection( this );
		}
	}

	private static class DynamicInstantiationQueryResultGenerator implements NonNavigableSelectionSupport.QueryResultGenerator {
		private final DynamicInstantiationSelection selection;

		public DynamicInstantiationQueryResultGenerator(DynamicInstantiationSelection selection) {
			this.selection = selection;
		}

		private <T> DynamicInstantiation<T> getDynamicInstantiation() {
			//noinspection unchecked
			return selection.getSelectedExpression();
		}

		@Override
		public QueryResult generateQueryResult(
				SqlSelectionResolver sqlSelectionResolver,
				QueryResultCreationContext creationContext) {
			boolean areAllArgumentsAliased = true;
			boolean areAnyArgumentsAliased = false;
			final Set<String> aliases = new HashSet<>();
			final List<String> duplicatedAliases = new ArrayList<>();
			final List<ArgumentReader> argumentReaders = new ArrayList<>();

			if ( getDynamicInstantiation().arguments != null ) {
				for ( DynamicInstantiationArgument argument : getDynamicInstantiation().arguments ) {
					if ( argument.getAlias() == null ) {
						areAllArgumentsAliased = false;
					}
					else {
						if ( !aliases.add( argument.getAlias() ) ) {
							duplicatedAliases.add( argument.getAlias() );
							log.debugf( "Query defined duplicate resultVariable encountered multiple declarations of [%s]", argument.getAlias() );
						}
						areAnyArgumentsAliased = true;
					}

					argumentReaders.add( argument.buildArgumentReader( sqlSelectionResolver, creationContext ) );
				}
			}

			final QueryResultAssembler assembler = resolveAssembler(
					getDynamicInstantiation(),
					areAllArgumentsAliased,
					areAnyArgumentsAliased,
					duplicatedAliases,
					argumentReaders,
					creationContext
			);

			return new QueryResultDynamicInstantiationImpl( getDynamicInstantiation(), selection.getResultVariable(), assembler );
		}
	}

	@SuppressWarnings("unchecked")
	private static QueryResultAssembler resolveAssembler(
			DynamicInstantiation dynamicInstantiation,
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader> argumentReaders,
			QueryResultCreationContext creationContext) {

		if ( dynamicInstantiation.getNature() == DynamicInstantiationNature.LIST ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new DynamicInstantiationListAssemblerImpl(
					(BasicJavaDescriptor<List>) dynamicInstantiation.getType().getJavaTypeDescriptor(),
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
					(BasicJavaDescriptor<Map>) dynamicInstantiation.getType().getJavaTypeDescriptor(),
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
						dynamicInstantiation.getType().getJavaTypeDescriptor(),
						argumentReaders
				);
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					dynamicInstantiation.getTargetJavaType().getName()
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

			return new DynamicInstantiationInjectionAssemblerImpl( dynamicInstantiation.getType().getJavaTypeDescriptor(), argumentReaders );
		}
	}
}
