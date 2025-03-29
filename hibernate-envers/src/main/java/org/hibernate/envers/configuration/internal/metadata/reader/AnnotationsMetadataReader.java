/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.SecondaryAuditTables;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Chris Cranford
 */
public final class AnnotationsMetadataReader {
	private final EnversMetadataBuildingContext metadataBuildingContext;

	public AnnotationsMetadataReader(EnversMetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	private RelationTargetAuditMode getDefaultAudited(ClassDetails classDetails) {
		final Audited defaultAudited = classDetails.getDirectAnnotationUsage( Audited.class );

		if ( defaultAudited != null ) {
			return defaultAudited.targetAuditMode();
		}
		else {
			return null;
		}
	}

	private void addAuditTable(ClassAuditingData auditData, ClassDetails classDetails) {
		final AuditTable auditTable = classDetails.getDirectAnnotationUsage( AuditTable.class );
		if ( auditTable != null ) {
			auditData.setAuditTable( auditTable );
		}
		else {
			auditData.setAuditTable( getDefaultAuditTable() );
		}
	}

	private void addAuditSecondaryTables(ClassAuditingData auditData, ClassDetails classDetails) {
		// Getting information on secondary tables
		final SecondaryAuditTable secondaryVersionsTable1 = classDetails.getDirectAnnotationUsage( SecondaryAuditTable.class );
		if ( secondaryVersionsTable1 != null ) {
			auditData.getSecondaryTableDictionary().put(
					secondaryVersionsTable1.secondaryTableName(),
					secondaryVersionsTable1.secondaryAuditTableName()
			);
		}

		final SecondaryAuditTables secondaryAuditTables = classDetails.getDirectAnnotationUsage( SecondaryAuditTables.class );
		if ( secondaryAuditTables != null ) {
			for ( SecondaryAuditTable secondaryAuditTable2 : secondaryAuditTables.value() ) {
				auditData.getSecondaryTableDictionary().put(
						secondaryAuditTable2.secondaryTableName(),
						secondaryAuditTable2.secondaryAuditTableName()
				);
			}
		}
	}

	public ClassAuditingData getAuditData(PersistentClass persistentClass) {
		final ClassAuditingData auditData = new ClassAuditingData( persistentClass );
		final ClassDetails classDetails = metadataBuildingContext.getClassDetailsRegistry().resolveClassDetails(
				persistentClass.getClassName()
		);

		final RelationTargetAuditMode auditMode = getDefaultAudited( classDetails );
		if ( auditMode != null ) {
			auditData.setDefaultAudited( true );
		}

		new AuditedPropertiesReader(
				metadataBuildingContext,
				PersistentPropertiesSource.forClass( persistentClass, classDetails ),
				auditData
		).read();

		addAuditTable( auditData, classDetails );
		addAuditSecondaryTables( auditData, classDetails );

		return auditData;
	}

	private final AuditTable defaultAuditTable = new AuditTable() {
		public String value() {
			return "";
		}

		public String schema() {
			return "";
		}

		public String catalog() {
			return "";
		}

		public Class<? extends Annotation> annotationType() {
			return this.getClass();
		}
	};

	private AuditTable getDefaultAuditTable() {
		return defaultAuditTable;
	}

}
