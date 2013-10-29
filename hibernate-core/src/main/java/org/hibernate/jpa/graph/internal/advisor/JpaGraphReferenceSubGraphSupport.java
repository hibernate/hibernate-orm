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
package org.hibernate.jpa.graph.internal.advisor;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.jpa.graph.spi.AttributeNodeImplementor;
import org.hibernate.loader.plan.spi.FetchOwner;

/**
 * @author Steve Ebersole
 */
abstract class JpaGraphReferenceSubGraphSupport implements JpaGraphReference {
	private static final Logger log = Logger.getLogger( JpaGraphReferenceSubGraphSupport.class );

	private final Map<String,AttributeNodeImplementor> elementGraphAttributeMap;


	protected JpaGraphReferenceSubGraphSupport(AttributeNodeImplementor<?> attributeNode) {
		this.elementGraphAttributeMap = new HashMap<String, AttributeNodeImplementor>();

		for ( Subgraph<?> subgraph : attributeNode.getSubgraphs().values() ) {
			for ( AttributeNode<?> subGraphAttributeNode : subgraph.getAttributeNodes() ) {
				final AttributeNodeImplementor<?> nodeImplementor = (AttributeNodeImplementor<?>) subGraphAttributeNode;
				final AttributeNodeImplementor<?> old = this.elementGraphAttributeMap.put(
						nodeImplementor.getAttributeName(),
						nodeImplementor
				);

				if ( old != null && old != nodeImplementor ) {
					throw new IllegalStateException(
							"Found multiple representations of the same attribute : " + nodeImplementor.getAttributeName()
					);
				}
			}
		}
	}

	@Override
	public JpaGraphReference attributeProcessed(String attributeName) {
		final AttributeNodeImplementor attributeNode = this.elementGraphAttributeMap.remove( attributeName );

		if ( attributeNode == null ) {
			return NoOpJpaGraphReference.INSTANCE;
		}

		return attributeNode.getAttribute().isCollection()
				? new JpaGraphCollectionReference( attributeNode )
				: new JpaGraphSingularAttributeReference( attributeNode );
	}

	@Override
	public void applyMissingFetches(FetchOwner fetchOwner) {
		for ( AttributeNodeImplementor attributeNode : elementGraphAttributeMap.values() ) {
			System.out.println(
					String.format(
							"Found unprocessed attribute node [%s], applying to fetch-owner [%s]",
							attributeNode.getAttributeName(),
							fetchOwner.getPropertyPath().getFullPath()
					)
			);

			log.tracef(
					"Found unprocessed attribute node [%s], applying to fetch-owner [%s]",
					attributeNode.getAttributeName(),
					fetchOwner.getPropertyPath()
			);

			AdviceHelper.buildFetch( fetchOwner, attributeNode );

			// todo : additionally we need to process any further graphs in the attribute node path
			//		since we are effectively at a leaf in the LoadPlan graph
		}
	}
}
