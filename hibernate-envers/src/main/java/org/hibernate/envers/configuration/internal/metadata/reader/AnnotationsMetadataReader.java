/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.SecondaryAuditTables;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;

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

	private RelationTargetAuditMode getDefaultAudited(XClass clazz) {
		final Audited defaultAudited = clazz.getAnnotation( Audited.class );

		if ( defaultAudited != null ) {
			return defaultAudited.targetAuditMode();
		}
		else {
			return null;
		}
	}

	private void addAuditTable(ClassAuditingData auditData, XClass clazz) {
		final AuditTable auditTable = clazz.getAnnotation( AuditTable.class );
		if ( auditTable != null ) {
			auditData.setAuditTable( auditTable );
		}
		else {
			auditData.setAuditTable( getDefaultAuditTable() );
		}
	}

	private void addAuditSecondaryTables(ClassAuditingData auditData, XClass clazz) {
		// Getting information on secondary tables
		final SecondaryAuditTable secondaryVersionsTable1 = clazz.getAnnotation( SecondaryAuditTable.class );
		if ( secondaryVersionsTable1 != null ) {
			auditData.getSecondaryTableDictionary().put(
					secondaryVersionsTable1.secondaryTableName(),
					secondaryVersionsTable1.secondaryAuditTableName()
			);
		}

		final SecondaryAuditTables secondaryAuditTables = clazz.getAnnotation( SecondaryAuditTables.class );
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
		final XClass xclass = metadataBuildingContext.getReflectionManager().toXClass( persistentClass.getMappedClass() );

		final RelationTargetAuditMode auditMode = getDefaultAudited( xclass );
		if ( auditMode != null ) {
			auditData.setDefaultAudited( true );
		}

		new AuditedPropertiesReader(
				metadataBuildingContext,
				PersistentPropertiesSource.forClass( persistentClass, xclass ),
				auditData
		).read();

		addAuditTable( auditData, xclass );
		addAuditSecondaryTables( auditData, xclass );

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
