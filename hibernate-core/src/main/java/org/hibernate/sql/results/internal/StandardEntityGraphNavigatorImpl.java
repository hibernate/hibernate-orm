/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Map;
import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.sql.results.graph.EntityGraphNavigator;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;

/**
 * @author Nathan Xu
 */
public class StandardEntityGraphNavigatorImpl implements EntityGraphNavigator {

	private final GraphSemantic graphSemantic;
	private GraphImplementor currentGraphContext;

	public StandardEntityGraphNavigatorImpl(EffectiveEntityGraph effectiveEntityGraph) {
		assert effectiveEntityGraph != null;
		if ( effectiveEntityGraph.getSemantic() == null ) {
			throw new IllegalArgumentException( "The graph has not defined semantic: " + effectiveEntityGraph );
		}
		this.graphSemantic = effectiveEntityGraph.getSemantic();
		this.currentGraphContext = effectiveEntityGraph.getGraph();
	}

	@Override
	public void backtrack(GraphImplementor previousContext) {
		currentGraphContext = previousContext;
	}

	@Override
	public Navigation navigateIfApplicable(FetchParent fetchParent, Fetchable fetchable, boolean exploreKeySubgraph) {
		final GraphImplementor previousContextRoot = currentGraphContext;
		FetchTiming fetchTiming = null;
		boolean joined = false;
		if ( appliesTo( fetchParent ) ) {
			final AttributeNodeImplementor attributeNode = currentGraphContext.findAttributeNode( fetchable.getFetchableName() );
			if ( attributeNode != null ) {
				fetchTiming = FetchTiming.IMMEDIATE;
				joined = true;

				final Map<Class<?>, SubGraphImplementor> subgraphMap;
				final Class<?> subgraphMapKey;

				if ( fetchable instanceof PluralAttributeMapping ) {
					PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;

					// avoid '^' bitwise operator to improve code readability
					assert exploreKeySubgraph && isJpaMapCollectionType( pluralAttributeMapping )
							|| !exploreKeySubgraph && !isJpaMapCollectionType( pluralAttributeMapping );

					if ( exploreKeySubgraph ) {
						subgraphMap = attributeNode.getKeySubGraphMap();
						subgraphMapKey = pluralAttributeMapping.getIndexDescriptor().getClass();
					}
					else {
						subgraphMap = attributeNode.getSubGraphMap();
						subgraphMapKey = pluralAttributeMapping.getElementDescriptor().getClass();
					}
				}
				else {
					assert !exploreKeySubgraph;
					subgraphMap = attributeNode.getSubGraphMap();
					subgraphMapKey = fetchable.getJavaTypeDescriptor().getJavaType();
				}
				currentGraphContext = subgraphMap == null ? null : subgraphMap.get( subgraphMapKey );
			}
			else {
				currentGraphContext = null;
			}
		}
		if ( fetchTiming == null ) {
			if ( graphSemantic == GraphSemantic.FETCH ) {
				fetchTiming = FetchTiming.DELAYED;
				joined = false;
			}
			else {
				fetchTiming = fetchable.getMappedFetchStrategy().getTiming();
				joined = fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;
			}
		}
		return new Navigation( previousContextRoot, fetchTiming, joined );
	}

	private boolean appliesTo(FetchParent fetchParent) {
		if ( currentGraphContext == null || !( fetchParent instanceof EntityResultGraphNode ) ) {
			return false;
		}

		final EntityResultGraphNode entityFetchParent = (EntityResultGraphNode) fetchParent;
		final EntityMappingType entityFetchParentMappingType = entityFetchParent.getEntityValuedModelPart().getEntityMappingType();

		assert currentGraphContext.getGraphedType() instanceof EntityDomainType;
		final EntityDomainType entityDomainType = (EntityDomainType) currentGraphContext.getGraphedType();

		return entityDomainType.getHibernateEntityName().equals( entityFetchParentMappingType.getEntityName() );
	}

	private static boolean isJpaMapCollectionType(PluralAttributeMapping pluralAttributeMapping) {
		return pluralAttributeMapping.getCollectionDescriptor().getCollectionSemantics().getCollectionClassification().toJpaClassification() == PluralAttribute.CollectionType.MAP;
	}

}
