/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.ast.expression.Expression;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.sqm.query.expression.Compatibility;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiation<T> implements Expression {
	private static final Logger log = Logger.getLogger( DynamicInstantiation.class );

	private final Class<T> target;
	private List<DynamicInstantiationArgument> arguments;
	boolean areAllArgumentsAliased = true;

	public DynamicInstantiation(Class<T> target) {
		this.target = target;
	}

	public void addArgument(String alias, Expression expression) {
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
			log.warnf(
					"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
							"but such aliases are needed for dynamic Map instantiations; " +
							"will likely cause problems later translating query",
					expression.toString(),
					alias
			);
		}

		areAllArgumentsAliased = areAllArgumentsAliased && alias != null;

		if ( arguments == null ) {
			arguments = new ArrayList<DynamicInstantiationArgument>();
		}
		arguments.add( new DynamicInstantiationArgument( alias, expression ) );
	}

	public List<DynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return "DynamicInstantiation(" + target.getName() + ")";
	}

	@Override
	public org.hibernate.type.Type getType() {
		return null;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitDynamicInstantiation( this );
	}

	@Override
	public ReturnReader getReturnReader(int startPosition, boolean shallow, SessionFactoryImplementor sessionFactory) {
		if ( List.class.equals( target ) ) {
			return new ReturnReaderDynamicInstantiationListImpl( arguments, startPosition, sessionFactory );
		}
		else if ( Map.class.equals( target ) ) {
			return new ReturnReaderDynamicInstantiationMapImpl( arguments, startPosition, sessionFactory );
		}
		else {
			List<AliasedReturnReader> argumentReaders = new ArrayList<AliasedReturnReader>();
			int numberOfColumnsConsumed = 0;
			for ( int i = 0; i < arguments.size(); i++ ) {
				final ReturnReader argumentReader = arguments.get( i ).getExpression().getReturnReader(
						startPosition + numberOfColumnsConsumed,
						true,
						sessionFactory
				);
				numberOfColumnsConsumed += argumentReader.getNumberOfColumnsRead( sessionFactory );
				argumentReaders.add( new AliasedReturnReader( arguments.get( i ).getAlias(), argumentReader ) );
			}

			// find a constructor matching argument types
			constructor_loop: for ( Constructor constructor : target.getDeclaredConstructors() ) {
				if ( constructor.getParameterTypes().length != arguments.size() ) {
					continue;
				}

				for ( int i = 0; i < arguments.size(); i++ ) {
					final ReturnReader argumentReader = argumentReaders.get( i ).getReturnReader();
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
				return new ReturnReaderDynamicInstantiationClassConstructorImpl( constructor, argumentReaders, numberOfColumnsConsumed );
			}

			log.debugf(
					"Could not locate appropriate constructor for dynamic instantiation of [%s]; attempting bean-injection instantiation",
					target.getName()
			);
			if ( !areAllArgumentsAliased ) {
				throw new InstantiationException(
						"Could not locate appropriate constructor for dynamic instantiation of class [" +
								target.getName() + "], and not all arguments were aliased so bean-injection could not be used"
				);
			}

			return new ReturnReaderDynamicInstantiationClassInjectionImpl( target, argumentReaders, numberOfColumnsConsumed );
		}
	}

}
