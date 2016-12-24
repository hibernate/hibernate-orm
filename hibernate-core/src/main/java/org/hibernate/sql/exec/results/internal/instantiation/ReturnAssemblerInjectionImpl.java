/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.internal.instantiation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sqm.query.expression.Compatibility;

/**
 * @author Steve Ebersole
 */
public class ReturnAssemblerInjectionImpl implements ReturnAssembler {
	private final Class target;
	private final List<BeanInjection> beanInjections = new ArrayList<>();

	public ReturnAssemblerInjectionImpl(Class target, List<ArgumentReader> argumentReaders) {
		this.target = target;

		BeanInfoHelper.visitBeanInfo(
				target,
				beanInfo -> {
					// needs to be ordered by argument order!
					//	todo : is this ^^ true anymore considering move to positional reads and SqlSelectionDescriptor?

					for ( ArgumentReader argumentReader : argumentReaders ) {
						boolean found = false;
						for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
							if ( argumentReader.getAlias().equals( propertyDescriptor.getName() ) ) {
								if ( propertyDescriptor.getWriteMethod() != null ) {
									final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
											propertyDescriptor.getWriteMethod().getParameterTypes()[0],
											argumentReader.getReturnedJavaType()
									);
									if ( assignmentCompatible ) {
										propertyDescriptor.getWriteMethod().setAccessible( true );
										beanInjections.add(
												new BeanInjection(
														new BeanInjectorSetter( propertyDescriptor.getWriteMethod() ),
														argumentReader
												)
										);
										found = true;
										break;
									}
								}
							}
						}
						if ( found ) {
							continue;
						}

						// see if we can find a Field with the given name...
						final Field field = findField( target, argumentReader.getAlias(), argumentReader.getReturnedJavaType() );
						if ( field != null ) {
							beanInjections.add(
									new BeanInjection(
											new BeanInjectorField( field ),
											argumentReader
									)
							);
						}
						else {
							throw new InstantiationException(
									"Unable to determine dynamic instantiation injection strategy for " +
											target.getName() + "#" + argumentReader.getAlias()
							);
						}
					}
				}
		);

		if ( argumentReaders.size() != beanInjections.size() ) {
			throw new IllegalStateException( "The number of readers did not match the number of injections" );
		}
	}

	private Field findField(Class declaringClass, String name, Class javaType) {
		try {
			Field field = declaringClass.getDeclaredField( name );
			// field should never be null
			if ( Compatibility.areAssignmentCompatible( field.getType(), javaType ) ) {
				field.setAccessible( true );
				return field;
			}
		}
		catch (NoSuchFieldException ignore) {
		}

		return null;
	}

	@Override
	public Class getReturnedJavaType() {
		return null;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		try {
			final Object result = target.newInstance();
			for ( BeanInjection beanInjection : beanInjections ) {
				beanInjection.getBeanInjector().inject(
						result,
						beanInjection.getValueAssembler().assemble( rowProcessingState, options )
				);
			}
			return result;
		}
		catch (IllegalAccessException | java.lang.InstantiationException e) {
			throw new InstantiationException( "Could not call default constructor [" + target.getSimpleName() + "]", e );
		}
	}
}
