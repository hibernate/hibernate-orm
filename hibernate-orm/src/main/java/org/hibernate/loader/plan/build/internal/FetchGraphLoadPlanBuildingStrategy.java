/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * Loadplan building strategy for {@link javax.persistence.EntityGraph} is applied in {@code javax.persistence.fetchgraph} mode.
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class FetchGraphLoadPlanBuildingStrategy extends AbstractEntityGraphVisitationStrategy {
	private final GraphNodeImplementor rootEntityGraph;

	public FetchGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,
			final LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers, lockMode );
		this.rootEntityGraph = (GraphNodeImplementor) loadQueryInfluencers.getFetchGraph();
	}

	@Override
	protected GraphNodeImplementor getRootEntityGraph() {
		return rootEntityGraph;
	}

	@Override
	protected FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition) {
		//under fetchgraph mode, according to the SPEC, all other attributes that no in entity graph are supposed to be lazily loaded
		return DEFAULT_LAZY;
	}

}
