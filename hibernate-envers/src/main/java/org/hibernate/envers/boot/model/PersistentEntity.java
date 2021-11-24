/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.mapping.PersistentClass;

/**
 * Contract for a persisted entity mapping.
 *
 * @author Chris Cranford
 */
public abstract class PersistentEntity implements AttributeContainer {

	private final AuditTableData auditTableData;
	private final PersistentClass persistentClass;

	public PersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		this.auditTableData = auditTableData;
		this.persistentClass = persistentClass;
	}

	public AuditTableData getAuditTableData() {
		return auditTableData;
	}

	protected PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public boolean isJoinAware() {
		return false;
	}

	public abstract void build(JaxbHbmHibernateMapping mapping);
}
