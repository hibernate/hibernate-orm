/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import java.io.Serializable;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public interface CollectionType<O,C,E> extends Type<C> {
	CollectionPersister<O,C,E> getCollectionPersister();

	@Override
	default Classification getClassification() {
		return Classification.COLLECTION;
	}

	@Override
	default Class<C> getJavaType() {
		return getCollectionPersister().getJavaType();
	}

	@Override
	default String asLoggableText() {
		return getCollectionPersister().asLoggableText();
	}

	@Override
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return null;
	}

	PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key);
}
