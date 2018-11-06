/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;

/**
 * Behaviours of different audit strategy for populating audit data.
 *
 * @deprecated (since 5.4), use {@link org.hibernate.envers.strategy.spi.AuditStrategy} instead.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
@Deprecated
public interface AuditStrategy extends org.hibernate.envers.strategy.spi.AuditStrategy {
	/**
	 * Perform the persistence of audited data for regular entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens
	 * @param enversService The EnversService
	 * @param id Id of the entity.
	 * @param data Audit data to persist
	 * @param revision Current revision data
	 * @deprecated (since 5.2.1), use {@link #perform(Session, String, AuditEntitiesConfiguration, Serializable, Object, Object)}
	 */
	@Deprecated
	default void perform(
			Session session,
			String entityName,
			EnversService enversService,
			Serializable id,
			Object data,
			Object revision) {
		perform(
				session,
				entityName,
				enversService.getAuditEntitiesConfiguration(),
				id,
				data,
				revision
		);
	}


	/**
	 * Perform the persistence of audited data for collection ("middle") entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens.
	 * @param propertyName The name of the property holding the persistent collection
	 * @param enversService The EnversService
	 * @param persistentCollectionChangeData Collection change data to be persisted.
	 * @param revision Current revision data
	 * @deprecated (since 5.2.1), use {@link #performCollectionChange(Session, String, String, AuditEntitiesConfiguration, PersistentCollectionChangeData, Object)}
	 */
	@Deprecated
	default void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			EnversService enversService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		performCollectionChange(
				session,
				entityName,
				propertyName,
				enversService.getAuditEntitiesConfiguration(),
				persistentCollectionChangeData,
				revision
		);
	}
}
