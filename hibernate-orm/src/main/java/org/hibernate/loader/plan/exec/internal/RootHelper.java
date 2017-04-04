/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
