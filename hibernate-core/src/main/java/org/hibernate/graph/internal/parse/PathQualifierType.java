/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;


import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
public enum PathQualifierType {

	KEY( (attributeNode, subtypeName, entityNameResolver) -> subtypeName == null
			? attributeNode.addKeySubgraph()
			: attributeNode.addKeySubgraph().addTreatedSubgraph( managedType( subtypeName, entityNameResolver ) )
	),

	VALUE( (attributeNode, subtypeName, entityNameResolver) -> subtypeName == null
			? attributeNode.addValueSubgraph()
			: attributeNode.addValueSubgraph().addTreatedSubgraph( managedType( subtypeName, entityNameResolver ) )
	);

	private static <T> ManagedDomainType<T> managedType(String subtypeName, EntityNameResolver entityNameResolver) {
		final var entityDomainType = entityNameResolver.resolveEntityName( subtypeName );
		if ( entityDomainType == null ) {
			throw new IllegalArgumentException( "Unknown managed type: " + subtypeName );
		}
		//noinspection unchecked
		return (ManagedDomainType<T>) entityDomainType;
	}

	private final SubGraphGenerator subGraphCreator;

	PathQualifierType(SubGraphGenerator subgraphCreator) {
		this.subGraphCreator = subgraphCreator;
	}

	public SubGraphGenerator getSubGraphCreator() {
		return subGraphCreator;
	}
}
