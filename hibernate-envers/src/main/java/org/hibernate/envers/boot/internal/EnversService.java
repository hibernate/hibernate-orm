/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcessManager;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

/**
 * Provides central access to Envers' configuration.
 *
 * In many ways, this replaces the legacy static map Envers used originally as
 * a means to share the old AuditConfiguration.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public interface EnversService extends Service {
	/**
	 * The name of the configuration setting used to control whether the Envers integration
	 * is enabled.  Default is true
	 */
	String INTEGRATION_ENABLED = "hibernate.integration.envers.enabled";

	/**
	 * Is the Envers integration enabled?  This is generally used as a
	 * protection for other Envers services (in the ServiceLoader sense)
	 * determine whether they should do their work.
	 *
	 * @return {@code true} If the integration is enabled; {@code false} otherwise.
	 */
	boolean isEnabled();

	/**
	 * Assuming {@link #isEnabled()} is {@code true}, has {@link #initialize}
	 * been called yet?
	 *
	 * @return {@code true} indicates {@link #initialize} has been called; {@code false}
	 * indicates that {@link #initialize} has not (yet) been called.
	 */
	boolean isInitialized();

	void initialize(
			MetadataImplementor metadata,
			MappingCollector mappingCollector,
			EffectiveMappingDefaults effectiveMappingDefaults);

	Configuration getConfig();

	AuditProcessManager getAuditProcessManager();

	AuditStrategy getAuditStrategy();

	EntitiesConfigurations getEntitiesConfigurations();

	RevisionInfoQueryCreator getRevisionInfoQueryCreator();

	RevisionInfoNumberReader getRevisionInfoNumberReader();

	ModifiedEntityNamesReader getModifiedEntityNamesReader();

	ClassLoaderService getClassLoaderService();

	ServiceRegistry getServiceRegistry();
}
