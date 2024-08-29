/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
