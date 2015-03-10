/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

	public InLineViewSourceImpl(
			MappingDocument mappingDocument,
			String schemaName,
			String catalogName,
			String selectStatement, String logicalName) {
		super( mappingDocument );
		this.schemaName = schemaName;
		this.catalogName = catalogName;
		this.selectStatement = selectStatement;
		this.logicalName = logicalName;
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
}
