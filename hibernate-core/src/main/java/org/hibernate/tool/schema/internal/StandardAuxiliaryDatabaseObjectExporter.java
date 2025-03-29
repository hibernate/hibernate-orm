/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardAuxiliaryDatabaseObjectExporter implements Exporter<AuxiliaryDatabaseObject> {
	private final Dialect dialect;

	public StandardAuxiliaryDatabaseObjectExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(AuxiliaryDatabaseObject object, Metadata metadata,
			SqlStringGenerationContext context) {
		return object.sqlCreateStrings( context );
	}

	@Override
	public String[] getSqlDropStrings(AuxiliaryDatabaseObject object, Metadata metadata,
			SqlStringGenerationContext context) {
		return object.sqlDropStrings( context );
	}
}
