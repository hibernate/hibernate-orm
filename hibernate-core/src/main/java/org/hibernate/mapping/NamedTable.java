/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.internal.QualifiedPhysicalNameSnapshot;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.ContributableDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;


/// Mapping for an object with a database name. Its name is finalized before construction.
///
/// @author Steve Ebersole
public abstract class NamedTable extends Table implements ContributableDatabaseObject {
	private transient QualifiedPhysicalName physicalName;
	private final QualifiedPhysicalNameSnapshot physicalNameSnapshot;

	protected NamedTable(String contributor, QualifiedPhysicalName name) {
		super( contributor );
		physicalName = java.util.Objects.requireNonNull( name );
		physicalNameSnapshot = QualifiedPhysicalNameSnapshot.from( name );
	}

	protected NamedTable(String contributor, Namespace namespace, PhysicalName name) {
		this( contributor, new QualifiedPhysicalName(
				namespace.getPhysicalName().catalog(), namespace.getPhysicalName().schema(), name ) );
	}

	public final QualifiedPhysicalName getPhysicalName() {
		if ( physicalName == null ) {
			throw new IllegalStateException( "Table physical names require attached system services" );
		}
		return physicalName;
	}

	public final void reattachPhysicalName(PhysicalName.Factory factory) {
		physicalName = physicalNameSnapshot.restore( factory );
	}

	@Override
	public String getTableExpression(SqlStringGenerationContext context) {
		return context.format( getPhysicalName() );
	}

	@Override
	public String getExportIdentifier() {
		return getPhysicalName().render();
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash( isView(), getPhysicalName() );
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		if ( !(other instanceof NamedTable that) || isView() != that.isView() ) {
			return false;
		}
		return getPhysicalName().equals( that.getPhysicalName() );
	}

	@Override
	public String getName() {
		return getPhysicalName().objectName().getText();
	}

	@Override
	public boolean isQuoted() { return getPhysicalName().objectName().isQuoted(); }
	@Override
	public String getSchema() { return getPhysicalName().schemaName() == null ? null : getPhysicalName().schemaName().getText(); }
	@Override
	public boolean isSchemaQuoted() { return getPhysicalName().schemaName() != null && getPhysicalName().schemaName().isQuoted(); }
	@Override
	public String getCatalog() { return getPhysicalName().catalogName() == null ? null : getPhysicalName().catalogName().getText(); }
	@Override
	public boolean isCatalogQuoted() { return getPhysicalName().catalogName() != null && getPhysicalName().catalogName().isQuoted(); }

	private String comment;
	private String options;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

}
