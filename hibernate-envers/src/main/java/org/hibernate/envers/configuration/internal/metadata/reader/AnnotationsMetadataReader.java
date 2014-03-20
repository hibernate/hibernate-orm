/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.event.spi.EnversDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class AnnotationsMetadataReader {

	private static final AuditTable DEFAULT_AUDIT_TABLE = new AuditTable() {
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

	private final AuditConfiguration.AuditConfigurationContext context;

	public AnnotationsMetadataReader(AuditConfiguration.AuditConfigurationContext context) {
		this.context = context;
	}

	private ModificationStore getDefaultAudited(ClassInfo classInfo) {
		final AnnotationInstance audited = JandexHelper.getSingleAnnotation(
				classInfo.annotations(),
				EnversDotNames.AUDITED,
				classInfo
		);
		if ( audited != null ) {
			return JandexHelper.getValue( audited, "modStore", ModificationStore.class, context.getClassLoaderService() );
		}
		return null;
	}

	private void addAuditTable(ClassInfo classInfo, ClassAuditingData auditData) {
		final AnnotationInstance auditTable = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.AUDIT_TABLE );
		if ( auditTable != null ) {
			auditData.setAuditTable(
					context.getAnnotationProxy(
							auditTable,
							AuditTable.class
					)
			);
		}
		else {
			auditData.setAuditTable( getDefaultAuditTable() );
		}
	}

	private void addAuditSecondaryTables(ClassInfo classInfo, ClassAuditingData auditData) {
		// Getting information on secondary tables
		final AnnotationInstance secondaryAuditTable1 = JandexHelper.getSingleAnnotation(
				classInfo, EnversDotNames.SECONDARY_AUDIT_TABLE
		);
		if ( secondaryAuditTable1 != null ) {
			auditData.getSecondaryTableDictionary().put(
					JandexHelper.getValue(
							secondaryAuditTable1, "secondaryTableName", String.class, context.getClassLoaderService()
					),
					JandexHelper.getValue(
							secondaryAuditTable1, "secondaryAuditTableName", String.class, context.getClassLoaderService()
					)
			);
		}

		final AnnotationInstance secondaryAuditTables = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.SECONDARY_AUDIT_TABLES );
		if ( secondaryAuditTables != null ) {
			final AnnotationInstance[] secondaryAuditTableValues =
					JandexHelper.getValue( secondaryAuditTables, "value", AnnotationInstance[].class, context.getClassLoaderService() );
			for ( AnnotationInstance secondaryAuditTable : secondaryAuditTableValues ) {
				auditData.getSecondaryTableDictionary().put(
						JandexHelper.getValue(
								secondaryAuditTable, "secondaryTableName", String.class, context.getClassLoaderService()
						),
						JandexHelper.getValue(
								secondaryAuditTable, "secondaryAuditTableName", String.class, context.getClassLoaderService()
						)
				);
			}
		}
	}

	public ClassAuditingData getAuditData(EntityBinding entityBinding) {
		/**
		 * This object is filled with information read from annotations and returned by the <code>getVersioningData</code>
		 * method.
		 */
		final ClassAuditingData auditData = new ClassAuditingData();

		if ( entityBinding.getEntity().getDescriptor() == null ) {
			// TODO: What is the case here? Test by throwing exception.
			return auditData;
		}

		final PersistentClassPropertiesSource persistentClassPropertiesSource = new PersistentClassPropertiesSource(
				entityBinding,
				context.getClassInfo(
						entityBinding.getEntity().getDescriptor().getName().toString()
				)
		);

		ModificationStore defaultStore = getDefaultAudited( persistentClassPropertiesSource.getClassInfo() );
		auditData.setDefaultAudited( defaultStore != null );

		new AuditedPropertiesReader(
				context,
				auditData,
				persistentClassPropertiesSource,
				""
		).read();

		addAuditTable( persistentClassPropertiesSource.getClassInfo(), auditData );
		addAuditSecondaryTables( persistentClassPropertiesSource.getClassInfo(), auditData );

		return auditData;
	}

	private AuditTable getDefaultAuditTable() {
		return DEFAULT_AUDIT_TABLE;
	}

	private class PersistentClassPropertiesSource implements PersistentPropertiesSource {
		private final EntityBinding entityBinding;
		private final ClassInfo classInfo;

		private PersistentClassPropertiesSource(EntityBinding entityBinding, ClassInfo classInfo) {
			this.entityBinding = entityBinding;
			this.classInfo = classInfo;
		}

		@SuppressWarnings({"unchecked"})
		public Iterable<AttributeBinding> getNonIdAttributeBindings() {
			return entityBinding.getNonIdAttributeBindings();
		}

		public AttributeBinding getAttributeBinding(String attributeName) {
			return entityBinding.locateAttributeBinding( attributeName );
		}

		@Override
		public AttributeBindingContainer getAttributeBindingContainer() {
			return entityBinding;
		}

		public ClassInfo getClassInfo() {
			return classInfo;
		}
	}
}
