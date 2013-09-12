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

/**
 * @author Steve Ebersole
 */
public class ConstructorReturn implements Return {
	private final Class targetClass;
	private final ScalarReturn[] scalars;

//	private final Constructor constructor;

	public ConstructorReturn(Class targetClass, ScalarReturn[] scalars) {
		this.targetClass = targetClass;
		this.scalars = scalars;

//		constructor = resolveConstructor( targetClass, scalars );
	}

	private static Constructor resolveConstructor(Class targetClass, ScalarReturn[] scalars) {
		for ( Constructor constructor : targetClass.getConstructors() ) {
			final Class[] argumentTypes = constructor.getParameterTypes();
			if ( argumentTypes.length != scalars.length ) {
				continue;
			}

			boolean allMatched = true;
			for ( int i = 0; i < argumentTypes.length; i++ ) {
				if ( areAssignmentCompatible( argumentTypes[i], scalars[i].getType().getReturnedClass() ) ) {
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
		// todo : add handling for primitive/wrapper equivalents
		return argumentType.isAssignableFrom( typeReturnedClass );
	}

	public Class getTargetClass() {
		return targetClass;
	}

	public ScalarReturn[] getScalars() {
		return scalars;
	}

//	public Constructor getConstructor() {
//		return constructor;
//	}
}
