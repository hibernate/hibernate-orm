/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.TableSource;

/**
 * Models a table mapping source.
 *
 * @author Steve Ebersole
 */
public class TableSourceImpl extends AbstractHbmSourceNode implements TableSource {
	private final String explicitCatalog;
	private final String explicitSchema;
	private final String explicitTableName;
	private final String rowId;
	private final String comment;
	private final String checkConstraint;

	TableSourceImpl(
			MappingDocument mappingDocument,
			String explicitSchema,
			String explicitCatalog,
			String explicitTableName,
			String rowId,
			String comment,
			String checkConstraint) {
		super( mappingDocument );
		this.explicitCatalog = explicitCatalog;
		this.explicitSchema = explicitSchema;
		this.explicitTableName = explicitTableName;
		this.rowId = rowId;
		this.comment = comment;
		this.checkConstraint = checkConstraint;
	}

	@Override
	public String getExplicitCatalogName() {
		return explicitCatalog;
	}

	@Override
	public String getExplicitSchemaName() {
		return explicitSchema;
	}

	@Override
	public String getExplicitTableName() {
		return explicitTableName;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getCheckConstraint() {
		return checkConstraint;
	}
}
