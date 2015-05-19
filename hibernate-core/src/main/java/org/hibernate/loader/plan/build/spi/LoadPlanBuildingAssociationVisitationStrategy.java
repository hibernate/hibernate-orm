/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.walking.spi.AssociationVisitationStrategy;

/**
 * Specialized {@link org.hibernate.persister.walking.spi.AssociationVisitationStrategy} implementation for
 * building {@link org.hibernate.loader.plan.spi.LoadPlan} instances.
 *
 * @author Steve Ebersole
 */
public interface LoadPlanBuildingAssociationVisitationStrategy extends AssociationVisitationStrategy {
	/**
	 * After visitation is done, build the load plan.
	 *
	 * @return The load plan
	 */
	public LoadPlan buildLoadPlan();
}
