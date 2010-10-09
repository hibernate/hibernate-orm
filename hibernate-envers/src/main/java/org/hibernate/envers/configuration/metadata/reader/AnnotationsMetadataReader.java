/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.configuration.metadata.reader;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.*;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.GlobalConfiguration;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class AnnotationsMetadataReader {
	private final GlobalConfiguration globalCfg;
	private final ReflectionManager reflectionManager;
	private final PersistentClass pc;

	/**
	 * This object is filled with information read from annotations and returned by the <code>getVersioningData</code>
	 * method.
	 */
	private final ClassAuditingData auditData;

	public AnnotationsMetadataReader(GlobalConfiguration globalCfg, ReflectionManager reflectionManager,
									 PersistentClass pc) {
		this.globalCfg = globalCfg;
		this.reflectionManager = reflectionManager;
		this.pc = pc;

		auditData = new ClassAuditingData();
	}

	private ModificationStore getDefaultAudited(XClass clazz) {
		Audited defaultAudited = clazz.getAnnotation(Audited.class);

		if (defaultAudited != null) {
			return defaultAudited.modStore();
		} else {
			return null;
		}
	}

	private void addAuditTable(XClass clazz) {
		AuditTable auditTable = clazz.getAnnotation(AuditTable.class);
		if (auditTable != null) {
			auditData.setAuditTable(auditTable);
		} else {
			auditData.setAuditTable(getDefaultAuditTable());
		}
	}

	private void addAuditSecondaryTables(XClass clazz) {
		// Getting information on secondary tables
		SecondaryAuditTable secondaryVersionsTable1 = clazz.getAnnotation(SecondaryAuditTable.class);
		if (secondaryVersionsTable1 != null) {
			auditData.getSecondaryTableDictionary().put(secondaryVersionsTable1.secondaryTableName(),
					secondaryVersionsTable1.secondaryAuditTableName());
		}

		SecondaryAuditTables secondaryAuditTables = clazz.getAnnotation(SecondaryAuditTables.class);
		if (secondaryAuditTables != null) {
			for (SecondaryAuditTable secondaryAuditTable2 : secondaryAuditTables.value()) {
				auditData.getSecondaryTableDictionary().put(secondaryAuditTable2.secondaryTableName(),
						secondaryAuditTable2.secondaryAuditTableName());
			}
		}
	}

	public ClassAuditingData getAuditData() {
		if (pc.getClassName() == null) {
			return auditData;
		}

		try {
			XClass xclass = reflectionManager.classForName(pc.getClassName(), this.getClass());

			ModificationStore defaultStore = getDefaultAudited(xclass);
			if (defaultStore != null) {
				auditData.setDefaultAudited(true);
			}

			new AuditedPropertiesReader(defaultStore, new PersistentClassPropertiesSource(xclass), auditData,
					globalCfg, reflectionManager, "").read();

			addAuditTable(xclass);
			addAuditSecondaryTables(xclass);
		} catch (ClassNotFoundException e) {
			throw new MappingException(e);
		}

		return auditData;
	}

	private AuditTable defaultAuditTable = new AuditTable() {
		public String value() { return ""; }
		public String schema() { return ""; }
		public String catalog() { return ""; }
		public Class<? extends Annotation> annotationType() { return this.getClass(); }
	};

	private AuditTable getDefaultAuditTable() {
		return defaultAuditTable;
	}

	private class PersistentClassPropertiesSource implements PersistentPropertiesSource {
		private final XClass xclass;

		private PersistentClassPropertiesSource(XClass xclass) { this.xclass = xclass; }

		@SuppressWarnings({"unchecked"})
		public Iterator<Property> getPropertyIterator() { return pc.getPropertyIterator(); }
		public Property getProperty(String propertyName) { return pc.getProperty(propertyName); }
		public XClass getXClass() { return xclass; }
	}
}
