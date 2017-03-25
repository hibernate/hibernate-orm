/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.InLineViewSource;

/**
 * @author Steve Ebersole
 */
public class InLineViewSourceImpl
		extends AbstractHbmSourceNode
		implements InLineViewSource {
	private final String schemaName;
	private final String catalogName;
	private final String selectStatement;
	private final String logicalName;
	private final String comment;

	public InLineViewSourceImpl(
			MappingDocument mappingDocument,
			String schemaName,
			String catalogName,
			String selectStatement, 
			String logicalName,
			String comment) {
		super( mappingDocument );
		this.schemaName = determineSchemaName( mappingDocument, schemaName );
		this.catalogName = determineCatalogName( mappingDocument, catalogName );
		this.selectStatement = selectStatement;
		this.logicalName = logicalName;
		this.comment = comment;
	}

	@Override
	public String getExplicitSchemaName() {
		return schemaName;
	}

	@Override
	public String getExplicitCatalogName() {
		return catalogName;
	}

	@Override
	public String getSelectStatement() {
		return selectStatement;
	}

	@Override
	public String getLogicalName() {
		return logicalName;
	}
	
	@Override
	public String getComment() {
		return comment;
	}

	private String determineCatalogName(MappingDocument mappingDocument, String catalogName) {
		return catalogName != null ? catalogName : mappingDocument.getDocumentRoot().getCatalog();
	}

	private String determineSchemaName(MappingDocument mappingDocument, String schemaName) {
		return schemaName != null ? schemaName : mappingDocument.getDocumentRoot().getSchema();
	}
}
