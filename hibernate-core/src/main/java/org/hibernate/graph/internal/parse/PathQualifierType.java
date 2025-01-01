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
			? attributeNode.makeKeySubGraph()
			: attributeNode.makeKeySubGraph( getSubtype( subtypeName, sessionFactory ) )
	),

	VALUE( (attributeNode, subtypeName, sessionFactory) -> subtypeName == null
			? attributeNode.makeSubGraph()
			: attributeNode.makeSubGraph( getSubtype( subtypeName, sessionFactory ) )
	);

	private static ManagedDomainType<?> getSubtype(String subtypeName, SessionFactoryImplementor sessionFactory) {
		final JpaMetamodel metamodel = sessionFactory.getJpaMetamodel();
		ManagedDomainType<?> managedType = metamodel.findManagedType( subtypeName );
		if ( managedType == null ) {
			managedType = metamodel.getHqlEntityReference( subtypeName );
		}
		if ( managedType == null ) {
			throw new IllegalArgumentException( "Unknown type " + subtypeName );
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
