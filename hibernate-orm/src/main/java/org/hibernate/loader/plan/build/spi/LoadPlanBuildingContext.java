/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationKey;

/**
 * Provides access to context needed in building a LoadPlan.
 *
 * @author Steve Ebersole
 */
public interface LoadPlanBuildingContext {
	/**
	 * Access to the SessionFactory
	 *
	 * @return The SessionFactory
	 */
	public SessionFactoryImplementor getSessionFactory();

	public ExpandingQuerySpaces getQuerySpaces();

	public FetchSource registeredFetchSource(AssociationKey associationKey);
}
