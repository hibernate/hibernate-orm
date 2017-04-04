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
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * Loadplan building strategy for {@link javax.persistence.EntityGraph} is applied in {@code javax.persistence.loadgraph} mode.
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class LoadGraphLoadPlanBuildingStrategy extends AbstractEntityGraphVisitationStrategy {
	private final GraphNodeImplementor rootEntityGraph;

	public LoadGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,final LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers , lockMode);
		this.rootEntityGraph = (GraphNodeImplementor) loadQueryInfluencers.getLoadGraph();
	}

	@Override
	protected GraphNodeImplementor getRootEntityGraph() {
		return rootEntityGraph;
	}

	@Override
	protected FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan(
				loadQueryInfluencers,
				currentPropertyPath
		);
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}

		return fetchStrategy;
	}

}
