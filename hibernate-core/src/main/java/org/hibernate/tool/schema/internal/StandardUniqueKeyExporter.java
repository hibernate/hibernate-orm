/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * An {@link Exporter} for {@linkplain UniqueKey unique constraints}.
 *
 * @author Brett Meyer
 */
public class StandardUniqueKeyExporter implements Exporter<UniqueKey> {
	private final Dialect dialect;

	public StandardUniqueKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(UniqueKey constraint, Metadata metadata, SqlStringGenerationContext context) {
		return new String[] { dialect.getUniqueDelegate()
				.getAlterTableToAddUniqueKeyCommand( constraint, metadata, context ) };
	}

	@Override
	public String[] getSqlDropStrings(UniqueKey constraint, Metadata metadata, SqlStringGenerationContext context) {
		return new String[] { dialect.getUniqueDelegate()
				.getAlterTableToDropUniqueKeyCommand( constraint, metadata, context ) };
	}
}
