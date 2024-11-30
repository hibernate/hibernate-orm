/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.configuration.internal.metadata.AuditEntityConfigurationRegistry;
import org.hibernate.envers.configuration.internal.metadata.AuditEntityNameRegister;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.service.ServiceRegistry;

/**
 * Metadata building context for Hibernate Envers
 *
 * @author Chris Cranford
 */
public interface EnversMetadataBuildingContext extends MetadataBuildingContext {
	Configuration getConfiguration();

	MappingCollector getMappingCollector();

	ServiceRegistry getServiceRegistry();

	SourceModelBuildingContext getSourceModelBuildingContext();

	ClassDetailsRegistry getClassDetailsRegistry();

	AuditEntityNameRegister getAuditEntityNameRegistry();

	AuditEntityConfigurationRegistry getAuditEntityConfigurationRegistry();
}
