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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.spi.AttributeNodeImplementor;
import org.hibernate.loader.plan.spi.FetchOwner;

/**
 * Models the root {@link javax.persistence.EntityGraph} as a JpaGraphReference
 *
 * @author Steve Ebersole
 */
class JpaGraphRootEntityReference implements JpaGraphReference {
	private static final Logger log = Logger.getLogger( JpaGraphRootEntityReference.class );

	private final Map<String,AttributeNodeImplementor> graphAttributeMap;

	JpaGraphRootEntityReference(EntityGraphImpl entityGraph) {
		graphAttributeMap = new HashMap<String, AttributeNodeImplementor>();

		final List<AttributeNodeImplementor<?>> explicitAttributeNodes = entityGraph.attributeImplementorNodes();
		if ( explicitAttributeNodes != null ) {
			for ( AttributeNodeImplementor node : explicitAttributeNodes ) {
				graphAttributeMap.put( node.getAttributeName(), node );
			}
		}
	}

	@Override
	public JpaGraphReference attributeProcessed(String attributeName) {
		final AttributeNodeImplementor attributeNode = graphAttributeMap.remove( attributeName );

		if ( attributeNode == null ) {
			return NoOpJpaGraphReference.INSTANCE;
		}

		return attributeNode.getAttribute().isCollection()
				? new JpaGraphCollectionReference( attributeNode )
				: new JpaGraphSingularAttributeReference( attributeNode );
	}


	@Override
	public void applyMissingFetches(FetchOwner fetchOwner) {
		for ( AttributeNodeImplementor attributeNode : graphAttributeMap.values() ) {
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
