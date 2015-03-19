/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcessManager;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.service.Service;

/**
 * Provides central access to Envers' configuration.
 *
 * In many ways, this replaces the legacy static map Envers used originally as
 * a means to share the old AuditConfiguration.
 *
 * @author Steve Ebersole
 */
public interface EnversService extends Service {
	/**
	 * The name of the configuration setting used to control whether the Envers integration
	 * is enabled.  Default is true
	 */
	public static final String INTEGRATION_ENABLED = "hibernate.integration.envers.enabled";

	/**
	 * The name of the legacy configuration setting used to control whether auto registration
	 * of envers listeners should happen or not.  Default is true
	 */
	public static final String LEGACY_AUTO_REGISTER = "hibernate.listeners.envers.autoRegister";

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

	void initialize(MetadataImplementor metadata, MappingCollector mappingCollector);

	GlobalConfiguration getGlobalConfiguration();

	AuditEntitiesConfiguration getAuditEntitiesConfiguration();

	AuditProcessManager getAuditProcessManager();

	AuditStrategy getAuditStrategy();

	EntitiesConfigurations getEntitiesConfigurations();

	RevisionInfoQueryCreator getRevisionInfoQueryCreator();

	RevisionInfoNumberReader getRevisionInfoNumberReader();

	ModifiedEntityNamesReader getModifiedEntityNamesReader();

	ClassLoaderService getClassLoaderService();
}
