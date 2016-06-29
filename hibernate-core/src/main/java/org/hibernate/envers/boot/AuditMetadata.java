/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot;

import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.strategy.AuditStrategy;

/**
 * Represents the Envers model based on all mapping sources.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditMetadata {
	AuditStrategy getAuditStrategy();
	EntitiesConfigurations getEntitiesConfigurations();
	RevisionInfoGenerator getRevisionInfoGenerator();
	RevisionInfoQueryCreator getRevisionInfoQueryCreator();
	RevisionInfoNumberReader getRevisionInfoNumberReader();
	ModifiedEntityNamesReader getModifiedEntityNamesReader();
	AuditMetadataBuildingOptions getAuditMetadataBuildingOptions();
}
