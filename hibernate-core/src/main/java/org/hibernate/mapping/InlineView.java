/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Objects;

import org.hibernate.relational.naming.internal.PhysicalNameSnapshot;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.relational.naming.spi.LogicalName;



/// Mapping for a subquery with a logical lookup name and no physical database object name.
///
/// @author Steve Ebersole
public final class InlineView extends Table {
	private final LogicalName logicalName;
	private final String query;
	private final PhysicalNameSnapshot catalog;
	private final PhysicalNameSnapshot schema;

	public InlineView(String contributor, LogicalName logicalName, String query) {
		this( contributor, logicalName, query, null );
	}

	public InlineView(String contributor, LogicalName logicalName, String query, Namespace namespace) {
		super( contributor );
		catalog = namespace == null ? null : PhysicalNameSnapshot.from( namespace.getPhysicalName().catalog() );
		schema = namespace == null ? null : PhysicalNameSnapshot.from( namespace.getPhysicalName().schema() );
		this.logicalName = Objects.requireNonNull( logicalName );
		if ( query == null || query.isBlank() ) {
			throw new IllegalArgumentException( "An inline view requires a nonblank query" );
		}
		this.query = query;
	}

	public LogicalName getLogicalName() {
		return logicalName;
	}

	@Override
	public String getName() {
		return logicalName.getText();
	}

	@Override
	public boolean isQuoted() { return logicalName.isQuoted(); }
	@Override
	public String getSchema() { return schema == null ? null : schema.text(); }
	@Override
	public boolean isSchemaQuoted() { return schema != null && schema.quoted(); }
	@Override
	public String getCatalog() { return catalog == null ? null : catalog.text(); }
	@Override
	public boolean isCatalogQuoted() { return catalog != null && catalog.quoted(); }

	@Override
	public String getSubselect() {
		return query;
	}

	@Override
	public String getTableExpression(SqlStringGenerationContext context) {
		return "( " + query + " )";
	}
}
