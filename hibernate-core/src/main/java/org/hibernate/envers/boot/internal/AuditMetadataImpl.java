/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.boot.spi.AuditMetadataImplementor;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.strategy.AuditStrategy;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditMetadataImpl implements AuditMetadataImplementor {

	private final AuditMetadataBuildingOptions options;
	private final RevisionInfoGenerator revisionInfoGenerator;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;
	private final EntitiesConfigurations entitiesConfigurations;

	public AuditMetadataImpl(
			AuditMetadataBuildingOptions options,
			RevisionInfoConfiguration revisionInfoConfiguration,
			EntitiesConfigurations entitiesConfigurations) {
		this.options = options;
		this.entitiesConfigurations = entitiesConfigurations;
		this.revisionInfoGenerator = revisionInfoConfiguration.getRevisionInfoGenerator();
		this.revisionInfoQueryCreator = revisionInfoConfiguration.getRevisionInfoQueryCreator();
		this.revisionInfoNumberReader = revisionInfoConfiguration.getRevisionInfoNumberReader();
		this.modifiedEntityNamesReader = revisionInfoConfiguration.getModifiedEntityNamesReader();
	}

	@Override
	public AuditMetadataBuildingOptions getAuditMetadataBuildingOptions() {
		return options;
	}

	@Override
	public AuditStrategy getAuditStrategy() {
		return options.getAuditStrategy();
	}

	@Override
	public EntitiesConfigurations getEntitiesConfigurations() {
		return entitiesConfigurations;
	}

	@Override
	public RevisionInfoGenerator getRevisionInfoGenerator() {
		return revisionInfoGenerator;
	}

	@Override
	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return revisionInfoQueryCreator;
	}

	@Override
	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return revisionInfoNumberReader;
	}

	@Override
	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return modifiedEntityNamesReader;
	}
}
