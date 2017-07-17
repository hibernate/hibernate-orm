/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.loading.internal;

import java.io.Serializable;
import java.sql.ResultSet;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;

/**
 * Represents a collection currently being loaded.
 *
 * @author Steve Ebersole
 */
public class LoadingCollectionEntry {
	private final ResultSet resultSet;
	private final PersistentCollectionDescriptor persister;
	private final Serializable key;
	private final PersistentCollection collection;

	LoadingCollectionEntry(
			ResultSet resultSet,
			PersistentCollectionDescriptor persister,
			Serializable key,
			PersistentCollection collection) {
		this.resultSet = resultSet;
		this.persister = persister;
		this.key = key;
		this.collection = collection;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public PersistentCollectionDescriptor getPersister() {
		return persister;
	}

	public Serializable getKey() {
		return key;
	}

	public PersistentCollection getCollection() {
		return collection;
	}

	@Override
	public String toString() {
		return getClass().getName() + "<rs=" + resultSet + ", coll=" + MessageHelper.collectionInfoString( persister.getNavigableRole().getFullPath(), key ) + ">@" + Integer.toHexString( hashCode() );
	}
}
