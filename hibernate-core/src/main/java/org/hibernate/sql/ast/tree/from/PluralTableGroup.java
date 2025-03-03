/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * @author Christian Beikov
 */
public interface PluralTableGroup extends TableGroup {

	PluralAttributeMapping getModelPart();

	TableGroup getElementTableGroup();

	TableGroup getIndexTableGroup();

	default TableGroup getTableGroup(CollectionPart.Nature nature) {
		switch ( nature ) {
			case ELEMENT:
				return getElementTableGroup();
			case INDEX:
				return getIndexTableGroup();
		}

		throw new IllegalStateException( "Could not find table group for: " + nature );
	}
}
