/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy;

import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;

/**
 * Behaviours of different audit strategy for populating audit data.
 *
 * @deprecated use {@link org.hibernate.envers.strategy.spi.AuditStrategy} instead.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
@Deprecated(since = "5.4")
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
	 * @deprecated use {@link org.hibernate.envers.strategy.spi.AuditStrategy#perform(org.hibernate.engine.spi.SharedSessionContractImplementor, String, Configuration, Object, Object, Object)}
	 */
	@Deprecated(since = "5.2.1")
	default void perform(
			Session session,
			String entityName,
			EnversService enversService,
			Object id,
			Object data,
			Object revision) {
		perform(
				(SharedSessionContractImplementor) session,
				entityName,
				enversService.getConfig(),
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
	 * @deprecated use {@link #performCollectionChange(SharedSessionContractImplementor, String, String, Configuration, PersistentCollectionChangeData, Object)}
	 */
	@Deprecated(since = "5.2.1")
	default void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			EnversService enversService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		performCollectionChange(
				(SharedSessionContractImplementor) session,
				entityName,
				propertyName,
				enversService.getConfig(),
				persistentCollectionChangeData,
				revision
		);
	}
}
