/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class EntityVersionMappingImpl extends BasicValuedSingularAttributeMapping implements EntityVersionMapping {
	public EntityVersionMappingImpl(
			String attributeName,
			String containingTableExpression,
			String mappedColumnExpression,
			BasicType basicType) {
		super( attributeName, containingTableExpression, mappedColumnExpression, null, basicType, basicType );
	}
}
