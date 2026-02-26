/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.AssociationInfo;
import org.hibernate.tool.reveng.internal.core.util.RevengUtils;

class CollectionPropertyBinder extends AbstractBinder {

	static CollectionPropertyBinder create(BinderContext binderContext) {
		return new CollectionPropertyBinder(binderContext);
	}

	private final PropertyBinder propertyBinder;

	private CollectionPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		this.propertyBinder = PropertyBinder.create(binderContext);
	}

	Property bind(
			String propertyName,
			Table table,
			ForeignKey fk,
			Collection value) {
		AssociationInfo associationInfo = determineAssociationInfo(fk);
		BinderUtils.updateFetchMode(value, associationInfo.getFetch());
		return propertyBinder.bind(table, propertyName, value, associationInfo);
	}

	private AssociationInfo determineAssociationInfo(
			ForeignKey foreignKey) {
		AssociationInfo origin = BinderUtils
				.getAssociationInfo(getRevengStrategy(), foreignKey, true);
		if(origin != null){
			return RevengUtils.createAssociationInfo(
					origin.getCascade() != null ? origin.getCascade() : "all",
					origin.getFetch(),
					origin.getInsert() != null ? origin.getInsert() : true,
					origin.getUpdate() != null ? origin.getUpdate() : true
				);
		}
		else {
			return RevengUtils.createAssociationInfo(null, null, true, true);
		}
	}

}
