/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionIndex<T extends Type> implements CollectionIndex<T> {
	private final CollectionPersister persister;
	private final T ormType;
	private final List<Column> columns;

	public AbstractCollectionIndex(CollectionPersister persister, T ormType, List<Column> columns) {
		this.persister = persister;
		this.ormType = ormType;
		this.columns = columns;
	}
	@Override
	public CollectionPersister getSource() {
		return persister;
	}

	@Override
	public String getTypeName() {
		return getOrmType().getName();
	}

	@Override
	public T getExportedDomainType() {
		return getOrmType();
	}

	@Override
	public Class getJavaType() {
		return getOrmType().getJavaType();
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public T getOrmType() {
		return ormType;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeIndex(" + persister.getRole() + " [" + getTypeName() + "])";
	}
}