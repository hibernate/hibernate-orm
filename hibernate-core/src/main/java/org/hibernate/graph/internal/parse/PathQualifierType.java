/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;


import static org.hibernate.graph.internal.parse.EntityNameResolver.managedType;

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

	private final SubGraphGenerator subGraphCreator;

	PathQualifierType(SubGraphGenerator subgraphCreator) {
		this.subGraphCreator = subgraphCreator;
	}

	public SubGraphGenerator getSubGraphCreator() {
		return subGraphCreator;
	}
}
