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
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * Loadplan building strategy for {@link javax.persistence.EntityGraph} is applied in {@code javax.persistence.fetchgraph} mode.
 *
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
public class FetchGraphLoadPlanBuildingStrategy extends AbstractEntityGraphVisitationStrategy {
	private final RootGraphImplementor rootEntityGraph;

	public FetchGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory,
			final LoadQueryInfluencers loadQueryInfluencers,
			final LockMode lockMode) {
		this( sessionFactory, loadQueryInfluencers.getEffectiveEntityGraph().getGraph(), loadQueryInfluencers, lockMode );
		assert loadQueryInfluencers.getEffectiveEntityGraph().getSemantic() == GraphSemantic.FETCH;
	}

	public FetchGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory,
			RootGraphImplementor graph,
			final LoadQueryInfluencers loadQueryInfluencers,
			final LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers, lockMode );
		this.rootEntityGraph = graph;
	}

	@Override
	protected RootGraphImplementor getRootEntityGraph() {
		return rootEntityGraph;
	}

	@Override
	protected FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition) {
		//under fetchgraph mode, according to the SPEC, all other attributes that no in entity graph are supposed to be lazily loaded
		return DEFAULT_LAZY;
	}

}
