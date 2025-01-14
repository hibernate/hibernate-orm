/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
public enum PathQualifierType {

	KEY( (attributeNode, subtypeName, sessionFactory) -> subtypeName == null
			? attributeNode.addKeySubgraph()
			: attributeNode.addKeySubgraph().addTreatedSubgraph( managedType( subtypeName, sessionFactory ) )
	),

	VALUE( (attributeNode, subtypeName, sessionFactory) -> subtypeName == null
			? attributeNode.addValueSubgraph()
			: attributeNode.addValueSubgraph().addTreatedSubgraph( managedType( subtypeName, sessionFactory ) )
	);

	private static <T> ManagedDomainType<T> managedType(String subtypeName, SessionFactoryImplementor sessionFactory) {
		final JpaMetamodel metamodel = sessionFactory.getJpaMetamodel();
		ManagedDomainType<T> managedType = metamodel.findManagedType( subtypeName );
		if ( managedType == null ) {
			managedType = metamodel.getHqlEntityReference( subtypeName );
		}
		if ( managedType == null ) {
			throw new IllegalArgumentException( "Unknown managed type: " + subtypeName );
		}
		return managedType;
	}

	private final SubGraphGenerator subGraphCreator;

	PathQualifierType(SubGraphGenerator subgraphCreator) {
		this.subGraphCreator = subgraphCreator;
	}

	public SubGraphGenerator getSubGraphCreator() {
		return subGraphCreator;
	}
}
