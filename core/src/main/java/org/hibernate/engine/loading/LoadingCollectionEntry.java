package org.hibernate.engine.loading;

import java.io.Serializable;
import java.sql.ResultSet;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Represents a collection currently being loaded.
 *
 * @author Steve Ebersole
 */
public class LoadingCollectionEntry {
	private final ResultSet resultSet;
	private final CollectionPersister persister;
	private final Serializable key;
	private final PersistentCollection collection;

	public LoadingCollectionEntry(
			ResultSet resultSet,
			CollectionPersister persister,
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

	public CollectionPersister getPersister() {
		return persister;
	}

	public Serializable getKey() {
		return key;
	}

	public PersistentCollection getCollection() {
		return collection;
	}

	public String toString() {
		return getClass().getName() + "<rs=" + resultSet + ", coll=" + MessageHelper.collectionInfoString( persister.getRole(), key ) + ">@" + Integer.toHexString( hashCode() );
	}
}
