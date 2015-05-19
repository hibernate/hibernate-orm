/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * A LoadPlan building strategy for cascade processing; meaning, it builds the LoadPlan for loading related to
 * cascading a particular action across associations
 *
 * @author Steve Ebersole
 */
public class CascadeStyleLoadPlanBuildingAssociationVisitationStrategy
		extends FetchStyleLoadPlanBuildingAssociationVisitationStrategy {
	private static final FetchStrategy EAGER = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	private static final FetchStrategy DELAYED = new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT );

	private final CascadingAction cascadeActionToMatch;

	/**
	 * Constructs a CascadeStyleLoadPlanBuildingAssociationVisitationStrategy.
	 *
	 * @param cascadeActionToMatch The particular cascading action that an attribute definition must match
	 *                             to eagerly fetch that attribute.
	 * @param sessionFactory The session factory
	 * @param loadQueryInfluencers The options which can influence the SQL query needed to perform the load.
	 * @param lockMode The lock mode.
	 */
	public CascadeStyleLoadPlanBuildingAssociationVisitationStrategy(
			CascadingAction cascadeActionToMatch,
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers, lockMode );
		this.cascadeActionToMatch = cascadeActionToMatch;
	}

	@Override
	protected FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition) {
		return attributeDefinition.determineCascadeStyle().doCascade( cascadeActionToMatch ) ? EAGER : DELAYED;
	}
}
