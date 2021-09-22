/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Map;
import java.util.Objects;
import jakarta.persistence.metamodel.PluralAttribute;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;

/**
 * @author Nathan Xu
 */
public class StandardEntityGraphTraversalStateImpl implements EntityGraphTraversalState {

	private final GraphSemantic graphSemantic;
	private GraphImplementor currentGraphContext;

	public StandardEntityGraphTraversalStateImpl(GraphSemantic graphSemantic, RootGraphImplementor rootGraphImplementor) {
		Objects.requireNonNull(graphSemantic, "graphSemantic cannot be null");
		Objects.requireNonNull( rootGraphImplementor, "rootGraphImplementor cannot be null" );
		this.graphSemantic = graphSemantic;
		this.currentGraphContext = rootGraphImplementor;
	}

	@Override
	public void backtrack(GraphImplementor previousContext) {
		currentGraphContext = previousContext;
	}

	@Override
	public TraversalResult traverse(FetchParent fetchParent, Fetchable fetchable, boolean exploreKeySubgraph) {
		assert !(fetchable instanceof CollectionPart);

		final GraphImplementor previousContextRoot = currentGraphContext;
		AttributeNodeImplementor attributeNode = null;
		if ( appliesTo( fetchParent ) ) {
			attributeNode = currentGraphContext.findAttributeNode( fetchable.getFetchableName() );
		}

		currentGraphContext = null;
		FetchTiming fetchTiming = null;
		boolean joined = false;

		if ( attributeNode != null ) {
			fetchTiming = FetchTiming.IMMEDIATE;
			joined = true;

			final Map<Class<?>, SubGraphImplementor> subgraphMap;
			final Class<?> subgraphMapKey;

			if ( fetchable instanceof PluralAttributeMapping ) {
				PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;

				assert exploreKeySubgraph && isJpaMapCollectionType( pluralAttributeMapping )
						|| !exploreKeySubgraph && !isJpaMapCollectionType( pluralAttributeMapping );

				if ( exploreKeySubgraph ) {
					subgraphMap = attributeNode.getKeySubGraphMap();
					subgraphMapKey = getEntityCollectionPartJavaClass( pluralAttributeMapping.getIndexDescriptor() );
				}
				else {
					subgraphMap = attributeNode.getSubGraphMap();
					subgraphMapKey = getEntityCollectionPartJavaClass( pluralAttributeMapping.getElementDescriptor() );
				}
			}
			else {
				assert !exploreKeySubgraph;
				subgraphMap = attributeNode.getSubGraphMap();
				subgraphMapKey = fetchable.getJavaTypeDescriptor().getJavaTypeClass();
			}
			if ( subgraphMap != null && subgraphMapKey != null ) {
				currentGraphContext = subgraphMap.get( subgraphMapKey );
			}
		}
		if ( fetchTiming == null ) {
			if ( graphSemantic == GraphSemantic.FETCH ) {
				fetchTiming = FetchTiming.DELAYED;
				joined = false;
			}
			else {
				fetchTiming = fetchable.getMappedFetchOptions().getTiming();
				joined = fetchable.getMappedFetchOptions().getStyle() == FetchStyle.JOIN;
			}
		}
		return new TraversalResult( previousContextRoot, fetchTiming, joined );
	}

	private Class<?> getEntityCollectionPartJavaClass(CollectionPart collectionPart) {
		if ( collectionPart instanceof EntityCollectionPart ) {
			EntityCollectionPart entityCollectionPart = (EntityCollectionPart) collectionPart;
			return entityCollectionPart.getEntityMappingType().getJavaTypeDescriptor().getJavaTypeClass();
		}
		else {
			return null;
		}
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
