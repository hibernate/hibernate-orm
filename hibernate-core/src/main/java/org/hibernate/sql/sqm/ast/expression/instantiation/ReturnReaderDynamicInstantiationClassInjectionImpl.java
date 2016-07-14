/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;
import org.hibernate.sqm.query.expression.Compatibility;

/**
 * @author Steve Ebersole
 */
public class ReturnReaderDynamicInstantiationClassInjectionImpl<T> implements ReturnReader<T> {
	private final Class<T> target;
	private final List<BeanInjection> beanInjections;
	private final int numberOfColumnsConsumed;

	public ReturnReaderDynamicInstantiationClassInjectionImpl(
			final Class<T> target,
			final List<AliasedReturnReader> aliasedArgumentReaders,
			int numberOfColumnsConsumed) {
		this.target = target;
		this.numberOfColumnsConsumed = numberOfColumnsConsumed;

		this.beanInjections = new ArrayList<BeanInjection>();

		BeanInfoHelper.visitBeanInfo(
				target,
				new BeanInfoHelper.BeanInfoDelegate() {
					@Override
					@SuppressWarnings("unchecked")
					public void processBeanInfo(BeanInfo beanInfo) throws Exception {
						// needs to be ordered by argument order!
						for ( AliasedReturnReader aliasedReturnReader : aliasedArgumentReaders ) {
							boolean found = false;
							for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
								if ( aliasedReturnReader.getAlias().equals( propertyDescriptor.getName() ) ) {
									if ( propertyDescriptor.getWriteMethod() != null ) {
										final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
												propertyDescriptor.getWriteMethod().getParameterTypes()[0],
												aliasedReturnReader.getReturnReader().getReturnedJavaType()
										);
										if ( assignmentCompatible ) {
											propertyDescriptor.getWriteMethod().setAccessible( true );
											beanInjections.add(
													new BeanInjection(
															new BeanInjectorSetter( propertyDescriptor.getWriteMethod() ),
															aliasedReturnReader.getReturnReader()
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
							final Field field = findField( target, aliasedReturnReader.getAlias(), aliasedReturnReader.getReturnReader().getReturnedJavaType() );
							if ( field != null ) {
								beanInjections.add(
										new BeanInjection(
												new BeanInjectorField( field ),
												aliasedReturnReader.getReturnReader()
										)
								);
							}
							else {
								throw new InstantiationException(
										"Unable to determine dynamic instantiation injection strategy for " +
												target.getName() + "#" + aliasedReturnReader.getAlias()
								);
							}
						}
					}
				}
		);

		assert aliasedArgumentReaders.size() == beanInjections.size();
	}

	private Field findField(Class<T> declaringClass, String name, Class javaType) {
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
	public void readBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( BeanInjection beanInjection : beanInjections ) {
			beanInjection.getValueReader().readBasicValues( processingState, options );
		}
	}

	@Override
	public void resolveBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( BeanInjection beanInjection : beanInjections ) {
			beanInjection.getValueReader().resolveBasicValues( processingState, options );
		}
	}

	@Override
	public T assemble(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		try {
			final T result = target.newInstance();

			for ( BeanInjection beanInjection : beanInjections ) {
				beanInjection.getBeanInjector().inject(
						result,
						beanInjection.getValueReader().assemble( processingState, options )
				);
			}
			return result;
		}
		catch (Exception e) {
			throw new InstantiationException( "Error performing dynamic instantiation : " + target.getName(), e );
		}
	}

	@Override
	public Class<T> getReturnedJavaType() {
		return target;
	}

	@Override
	public int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory) {
		return numberOfColumnsConsumed;
	}
}
