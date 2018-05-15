/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.service.Service;

/**
 * Service contract for auditing services provided by Envers.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditService extends Service {
	/**
	 * Initialize the audit service.
	 *
	 * @param auditMetadata The compiled audit metadata.
	 */
	void initialize(AuditMetadata auditMetadata);

	/**
	 * Get the {@link AuditServiceOptions}, a set of immutable configuration state.
	 *
	 * @return The audit service options.
	 */
	AuditServiceOptions getOptions();

	/**
	 * Get an {@link AuditProcess} associated with the specified session.
	 *
	 * @param session the session
	 * @return The AuditProcess
	 */
	AuditProcess getAuditProcess(EventSource session);

	EntitiesConfigurations getEntityBindings();

	RevisionInfoNumberReader getRevisionInfoNumberReader();

	RevisionInfoQueryCreator getRevisionInfoQueryCreator();

	ModifiedEntityNamesReader getModifiedEntityNamesReader();

	String getAuditEntityName(String entityName);

	String getRevisionPropertyPath(String propertyName);

	ClassLoaderService getClassLoaderService();
}
