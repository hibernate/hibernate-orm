/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public interface PropertyMapper extends ModifiedFlagMapperSupport {
	/**
	 * Maps properties to the given map, basing on differences between properties of new and old objects.
	 *
	 * @param session The current session.
	 * @param data Data to map to.
	 * @param newObj New state of the entity.
	 * @param oldObj Old state of the entity.
	 *
	 * @return True if there are any differences between the states represented by newObj and oldObj.
	 */
	boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj);

	/**
	 * Maps properties from the given map to the given object.
	 *
	 * @param enversService The EnversService.
	 * @param obj Object to map to.
	 * @param data Data to map from.
	 * @param primaryKey Primary key of the object to which we map (for relations)
	 * @param versionsReader VersionsReader for reading relations
	 * @param revision Revision at which the object is read, for reading relations
	 */
	void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision);

	/**
	 * Maps collection changes.
	 *
	 * @param session The current session.
	 * @param referencingPropertyName Name of the field, which holds the collection in the entity.
	 * @param newColl New collection, afterQuery updates.
	 * @param oldColl Old collection, beforeQuery updates.
	 * @param id Id of the object owning the collection.
	 *
	 * @return List of changes that need to be performed on the persistent store.
	 */
	List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session, String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id);

	void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj);

	void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data);
}
