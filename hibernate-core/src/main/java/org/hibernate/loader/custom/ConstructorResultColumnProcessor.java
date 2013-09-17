/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.custom;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
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
		final List<Type> localTypes = new ArrayList<Type>();
		for ( ScalarResultColumnProcessor scalar : scalarProcessors ) {
			scalar.performDiscovery( metadata, localTypes, aliases );
		}

		types.addAll( localTypes );

		constructor = resolveConstructor( targetClass, localTypes );
	}

	@Override
	public Object extract(Object[] data, ResultSet resultSet, SessionImplementor session)
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
		catch (InvocationTargetException e) {
			throw new HibernateException(
					String.format( "Unable to call %s constructor", constructor.getDeclaringClass() ),
					e
			);
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
