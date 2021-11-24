/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Chris Cranford
 */
public abstract class SubclassPersistentEntity extends PersistentEntity {

	private String extendsEntityName;

	public SubclassPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
	}

	public String getExtends() {
		return extendsEntityName;
	}

	public void setExtends(String extendsEntityName) {
		this.extendsEntityName = extendsEntityName;
	}

}
