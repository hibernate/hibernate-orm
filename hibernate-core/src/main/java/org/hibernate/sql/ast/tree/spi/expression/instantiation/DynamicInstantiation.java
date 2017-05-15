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
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.persister.queryable.spi.TableGroupResolver;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.ast.consume.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.ast.consume.results.internal.instantiation.QueryResultAssemblerConstructorImpl;
import org.hibernate.sql.ast.consume.results.internal.instantiation.QueryResultAssemblerInjectionImpl;
import org.hibernate.sql.ast.consume.results.internal.instantiation.QueryResultAssemblerListImpl;
import org.hibernate.sql.ast.consume.results.internal.instantiation.QueryResultAssemblerMapImpl;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.result.internal.QueryResultDynamicInstantiationImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.QueryResultGenerator;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.internal.NonNavigableSelectionSupport;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements Expression, Selectable {
	private static final Logger log = Logger.getLogger( DynamicInstantiation.class );

	private final Class<T> target;
	private List<DynamicInstantiationArgument> arguments;

	public DynamicInstantiation(Class<T> target) {
		this.target = target;
	}

	public Class<T> getTarget() {
		return target;
	}

	public void addArgument(String alias, Expression expression) {
		if ( queryReturn != null ) {
			throw new ConversionException( "Unexpected call to DynamicInstantiation#addAgument after query Return already built" );
		}

		if ( List.class.equals( target ) ) {
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
		else if ( Map.class.equals( target ) ) {
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
	}

	public List<DynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return "DynamicInstantiation(" + target.getName() + ")";
	}

	@Override
	public ExpressableType getType() {
		return null;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitDynamicInstantiation( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	private QueryResultDynamicInstantiationImpl queryReturn;

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression == this;

		return new DynamicInstantiationSelection(
				this,
				resultVariable
		);
	}

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
	}

	private static class DynamicInstantiationQueryResultGenerator implements QueryResultGenerator {
		private final DynamicInstantiationSelection dynamicInstantiationSelection;

		public DynamicInstantiationQueryResultGenerator(DynamicInstantiationSelection dynamicInstantiationSelection) {
			this.dynamicInstantiationSelection = dynamicInstantiationSelection;
		}

		private <T> DynamicInstantiation<T> getDynamicInstantiation() {
			//noinspection unchecked
			return dynamicInstantiationSelection.getSelectedExpression();
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
						}
						areAnyArgumentsAliased = true;
					}

					argumentReaders.add( argument.buildArgumentReader( sqlSelectionResolver, creationContext ) );
				}
			}

			final QueryResultAssembler assembler = resolveAssembler(
					getDynamicInstantiation().target,
					getDynamicInstantiation().arguments,
					areAllArgumentsAliased,
					areAnyArgumentsAliased,
					duplicatedAliases,
					argumentReaders
			);

			return new QueryResultDynamicInstantiationImpl( getDynamicInstantiation(), dynamicInstantiationSelection.getResultVariable(), assembler );
		}
	}

	private static QueryResultAssembler resolveAssembler(
			Class target,
			List<DynamicInstantiationArgument> arguments,
			boolean areAllArgumentsAliased,
			boolean areAnyArgumentsAliased,
			List<String> duplicatedAliases,
			List<ArgumentReader> argumentReaders) {
		if ( List.class.equals( target ) ) {
			if ( log.isDebugEnabled() && areAnyArgumentsAliased ) {
				log.debug( "One or more arguments for List dynamic instantiation (`new list(...)`) specified an alias; ignoring" );
			}
			return new QueryResultAssemblerListImpl( argumentReaders );
		}
		else if ( Map.class.equals( target ) ) {
			if ( ! areAllArgumentsAliased ) {
				throw new IllegalStateException( "Map dynamic instantiation contained one or more arguments with no alias" );
			}
			if ( !duplicatedAliases.isEmpty() ) {
				throw new IllegalStateException(
						"Map dynamic instantiation contained arguments with duplicated aliases [" + StringHelper.join( ",", duplicatedAliases ) + "]"
				);
			}
			return new QueryResultAssemblerMapImpl( argumentReaders );
		}
		else {
			// find a constructor matching argument types
			constructor_loop:
			for ( Constructor constructor : target.getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != arguments.size() ) {
					continue;
				}

				for ( int i = 0; i < arguments.size(); i++ ) {
					final ArgumentReader argumentReader = argumentReaders.get( i );
					// todo : move Compatibility from SQM into ORM?  It is only used here
					final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
							constructor.getParameterTypes()[i],
							argumentReader.getReturnedJavaType()
					);
					if ( !assignmentCompatible ) {
						log.debugf(
								"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
								i,
								constructor.getParameterTypes()[i],
								argumentReader.getReturnedJavaType()
						);
						continue constructor_loop;
					}
				}

				constructor.setAccessible( true );
				return new QueryResultAssemblerConstructorImpl( constructor, argumentReaders );
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					target.getName()
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

			return new QueryResultAssemblerInjectionImpl( target, argumentReaders );
		}
	}
}
