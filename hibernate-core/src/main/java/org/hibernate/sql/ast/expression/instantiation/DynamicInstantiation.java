/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.instantiation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.convert.ConversionException;
import org.hibernate.sql.convert.results.internal.ReturnDynamicInstantiationImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerConstructorImpl;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerInjectionImpl;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerListImpl;
import org.hibernate.sql.exec.results.internal.instantiation.ReturnAssemblerMapImpl;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.sqm.query.expression.Compatibility;
import org.hibernate.type.spi.Type;

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
	public Type getType() {
		return null;
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitDynamicInstantiation( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Expression getSelectedExpression() {
		return this;
	}

	private ReturnDynamicInstantiationImpl queryReturn;

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		if ( queryReturn != null ) {
			return queryReturn;
		}

		boolean areAllArgumentsAliased = true;
		boolean areAnyArgumentsAliased = false;
		final Set<String> aliases = new HashSet<>();
		final List<String> duplicatedAliases = new ArrayList<>();

		final List<ArgumentReader> argumentReaders = new ArrayList<>();
		if ( arguments != null ) {
			for ( DynamicInstantiationArgument argument : arguments ) {
				if ( argument.getAlias() == null ) {
					areAllArgumentsAliased = false;
				}
				else {
					if ( !aliases.add( argument.getAlias() ) ) {
						duplicatedAliases.add( argument.getAlias() );
					}
					areAnyArgumentsAliased = true;
				}

				argumentReaders.add( argument.buildArgumentReader( returnResolutionContext ) );
			}
		}

		final ReturnAssembler assembler = resolveAssembler(
				target,
				arguments,
				areAllArgumentsAliased,
				areAnyArgumentsAliased,
				duplicatedAliases,
				argumentReaders
		);

		queryReturn = new ReturnDynamicInstantiationImpl( this, resultVariable, assembler );

		return queryReturn;
	}

	private static ReturnAssembler resolveAssembler(
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
			return new ReturnAssemblerListImpl( argumentReaders );
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
			return new ReturnAssemblerMapImpl( argumentReaders );
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
				return new ReturnAssemblerConstructorImpl( constructor, argumentReaders );
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

			return new ReturnAssemblerInjectionImpl( target, argumentReaders );
		}
	}
}
