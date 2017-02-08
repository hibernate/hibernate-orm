/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.sqm.domain.SqmPluralAttributeIndex;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface CollectionIndex<J,T extends Type<J>>
		extends TypeExporter<J>, Navigable<J>, SqmPluralAttributeIndex {

	String NAVIGABLE_NAME = "{index}";

	List<Column> getColumns();

	@Override
	T getOrmType();
}
