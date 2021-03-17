/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;

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
									subTypeName
							)
					)
	),
	VALUE(
			(attributeNode, subTypeName, sessionFactory) ->
					attributeNode.makeSubGraph(
							resolveSubTypeManagedType(
									attributeNode.getAttributeDescriptor().getValueGraphType(),
									subTypeName
							)
					)
	);

	private static ManagedTypeDescriptor resolveSubTypeManagedType(
			SimpleTypeDescriptor<?> graphType,
			String subTypeName) {
		if ( !( graphType instanceof ManagedTypeDescriptor ) ) {
			throw new CannotContainSubGraphException( "The given type [" + graphType + "] is not a ManagedType" );
		}

		ManagedTypeDescriptor managedType = (ManagedTypeDescriptor) graphType;

		if ( subTypeName != null ) {
			managedType = managedType.findSubType( subTypeName );
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
