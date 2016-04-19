/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.type.PrimitiveWrapperHelper;
import org.hibernate.type.Type;

/**
 * Represents a {@link javax.persistence.ConstructorResult} within the custom query.
 *
 * @author Steve Ebersole
 */
public class ConstructorResultColumnProcessor implements ResultColumnProcessor {
	private final Class targetClass;
	private final ScalarResultColumnProcessor[] scalarProcessors;

	private Constructor constructor;

	public ConstructorResultColumnProcessor(Class targetClass, ScalarResultColumnProcessor[] scalarProcessors) {
		this.targetClass = targetClass;
		this.scalarProcessors = scalarProcessors;
	}

	@Override
	public void performDiscovery(JdbcResultMetadata metadata, List<Type> types, List<String> aliases) throws SQLException {
		final List<Type> localTypes = new ArrayList<>();
		for ( ScalarResultColumnProcessor scalar : scalarProcessors ) {
			scalar.performDiscovery( metadata, localTypes, aliases );
		}

		types.addAll( localTypes );

		constructor = resolveConstructor( targetClass, localTypes );
	}

	@Override
	public Object extract(Object[] data, ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException, HibernateException {
		if ( constructor == null ) {
			throw new IllegalStateException( "Constructor to call was null" );
		}

		final Object[] args = new Object[ scalarProcessors.length ];
		for ( int i = 0; i < scalarProcessors.length; i++ ) {
			args[i] = scalarProcessors[i].extract( data, resultSet, session );
		}

		try {
			return constructor.newInstance( args );
		}
		catch (Exception e) {
			throw new HibernateException(
					String.format( "Unable to call %s constructor", constructor.getDeclaringClass() ),
					e
			);
		}
	}

	private static Constructor resolveConstructor(Class targetClass, List<Type> types) {
		for ( Constructor constructor : targetClass.getConstructors() ) {
			final Class[] argumentTypes = constructor.getParameterTypes();
			if ( argumentTypes.length != types.size() ) {
				continue;
			}

			boolean allMatched = true;
			for ( int i = 0; i < argumentTypes.length; i++ ) {
				if ( ! areAssignmentCompatible( argumentTypes[i], types.get( i ).getReturnedClass() ) ) {
					allMatched = false;
					break;
				}
			}
			if ( !allMatched ) {
				continue;
			}

			return constructor;
		}

		throw new IllegalArgumentException( "Could not locate appropriate constructor on class : " + targetClass.getName() );
	}

	@SuppressWarnings("unchecked")
	private static boolean areAssignmentCompatible(Class argumentType, Class typeReturnedClass) {
		return argumentType.isAssignableFrom( typeReturnedClass )
				|| PrimitiveWrapperHelper.arePrimitiveWrapperEquivalents( argumentType, typeReturnedClass );
	}
}
