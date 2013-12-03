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
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.loader.plan.spi.EntityQuerySpace;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaces;
import org.hibernate.loader.plan.spi.Return;

/**
 * @author Steve Ebersole
 */
public class RootHelper {
	/**
	 * Singleton access
	 */
	public static final RootHelper INSTANCE = new RootHelper();

	/**
	 * Disallow direct instantiation
	 */
	private RootHelper() {
	}


	/**
	 * Extract the root return of the LoadPlan, assuming there is just one.
	 *
	 * @param loadPlan The LoadPlan from which to extract the root return
	 * @param returnType The Return type expected, passed as an argument
	 * @param <T> The parameterized type of the specific Return type expected
	 *
	 * @return The root Return
	 *
	 * @throws IllegalStateException If there is no root, more than one root or the single root
	 * is not of the expected type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Return> T extractRootReturn(LoadPlan loadPlan, Class<T> returnType) {
		if ( loadPlan.getReturns().size() == 0 ) {
			throw new IllegalStateException( "LoadPlan contained no root returns" );
		}
		else if ( loadPlan.getReturns().size() > 1 ) {
			throw new IllegalStateException( "LoadPlan contained more than one root returns" );
		}

		final Return rootReturn = loadPlan.getReturns().get( 0 );
		if ( !returnType.isInstance( rootReturn ) ) {
			throw new IllegalStateException(
					String.format(
							"Unexpected LoadPlan root return; expecting %s, but found %s",
							returnType.getName(),
							rootReturn.getClass().getName()
					)
			);
		}

		return (T) rootReturn;
	}

	/**
	 * Extract the root QuerySpace of the LoadPlan, assuming there is just one.
	 *
	 *
	 * @param querySpaces The QuerySpaces from which to extract the root.
	 * @param returnType The QuerySpace type expected, passed as an argument
	 *
	 * @return The root QuerySpace
	 *
	 * @throws IllegalStateException If there is no root, more than one root or the single root
	 * is not of the expected type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends QuerySpace> T extractRootQuerySpace(QuerySpaces querySpaces, Class<EntityQuerySpace> returnType) {
		if ( querySpaces.getRootQuerySpaces().size() == 0 ) {
			throw new IllegalStateException( "LoadPlan contained no root query-spaces" );
		}
		else if ( querySpaces.getRootQuerySpaces().size() > 1 ) {
			throw new IllegalStateException( "LoadPlan contained more than one root query-space" );
		}

		final QuerySpace querySpace = querySpaces.getRootQuerySpaces().get( 0 );
		if ( !returnType.isInstance( querySpace ) ) {
			throw new IllegalStateException(
					String.format(
							"Unexpected LoadPlan root query-space; expecting %s, but found %s",
							returnType.getName(),
							querySpace.getClass().getName()
					)
			);
		}

		return (T) querySpace;
	}
}
