/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import java.util.List;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.collection.spi.CollectionIndex;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionIndex<O extends Type> implements CollectionIndex {
	private final CollectionPersister persister;
	private final O ormType;
	private final List<Column> columns;

	public AbstractCollectionIndex(CollectionPersister persister, O ormType, List<Column> columns) {
		this.persister = persister;
		this.ormType = ormType;
		this.columns = columns;
	}

	@Override
	public O getOrmType() {
		return ormType;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeIndex(" + persister.getRole() + " [" + getOrmType().getName() + "])";
	}
}