/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;

import static org.hibernate.query.sqm.tree.expression.Compatibility.areAssignmentCompatible;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class InstantiationHelper {

	private static final Logger log = Logger.getLogger( InstantiationHelper.class );

	private InstantiationHelper() {
		// disallow direct instantiation
	}

	public static boolean isConstructorCompatible(
			Constructor<?> constructor,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
		if ( genericParameterTypes.length == argumentTypes.size() ) {
			for (int i = 0; i < argumentTypes.size(); i++ ) {
				final Type parameterType = genericParameterTypes[i];
				final Class<?> argumentType = argumentTypes.get( i );
				final Class<?> argType = parameterType instanceof Class<?>
						? (Class<?>) parameterType
						: typeConfiguration.getJavaTypeRegistry().resolveDescriptor( parameterType ).getJavaTypeClass();

                if ( !areAssignmentCompatible( argType, argumentType ) ) {
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
								i,
								argumentType.getTypeName(),
								parameterType.getTypeName()
						);
					}
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
}
