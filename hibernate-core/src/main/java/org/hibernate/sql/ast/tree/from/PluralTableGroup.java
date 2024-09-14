/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
