/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;


/**
 * Holds information necessary to create an audit table: its name, schema and catalog, as well as the audit
 * entity name.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditTableData {
	private final String auditEntityName;
	private final String auditTableName;
	private final String schema;
	private final String catalog;

	public AuditTableData(String auditEntityName, String auditTableName, String schema, String catalog) {
		this.auditEntityName = auditEntityName;
		this.auditTableName = auditTableName;
		this.schema = schema;
		this.catalog = catalog;
	}

	public String getAuditEntityName() {
		return auditEntityName;
	}

	public String getAuditTableName() {
		return auditTableName;
	}

	public String getSchema() {
		return schema;
	}

	public String getCatalog() {
		return catalog;
	}
}
