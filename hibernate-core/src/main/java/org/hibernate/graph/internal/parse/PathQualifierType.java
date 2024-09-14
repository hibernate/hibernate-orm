/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

import static org.hibernate.metamodel.model.domain.internal.DomainModelHelper.resolveSubType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public enum PathQualifierType {
	KEY(
			(attributeNode, subTypeName, sessionFactory) ->
					attributeNode.makeKeySubGraph(
							resolveSubTypeManagedType(
									attributeNode.getAttributeDescriptor().getKeyGraphType(),
									subTypeName,
									sessionFactory.getJpaMetamodel()
							)
					)
	),
	VALUE(
			(attributeNode, subTypeName, sessionFactory) ->
					attributeNode.makeSubGraph(
							resolveSubTypeManagedType(
									attributeNode.getAttributeDescriptor().getValueGraphType(),
									subTypeName,
									sessionFactory.getJpaMetamodel()
							)
					)
	);

	private static ManagedDomainType resolveSubTypeManagedType(
			DomainType<?> graphType,
			String subTypeName,
			JpaMetamodel metamodel) {
		if ( !( graphType instanceof ManagedDomainType<?> managedType ) ) {
			throw new CannotContainSubGraphException( "The given type [" + graphType + "] is not a ManagedType" );
		}

		if ( subTypeName != null ) {
			managedType = resolveSubType( managedType, subTypeName, metamodel );
		}
		return managedType;
	}

	private final SubGraphGenerator subGraphCreator;

	PathQualifierType(SubGraphGenerator subGraphCreator) {
		this.subGraphCreator = subGraphCreator;
	}

	public SubGraphGenerator getSubGraphCreator() {
		return subGraphCreator;
	}
}
