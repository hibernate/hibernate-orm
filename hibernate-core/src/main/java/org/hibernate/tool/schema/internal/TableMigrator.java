/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * An object that produces an {@code alter table} statements
 * needed to update the definition of a table.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public interface TableMigrator {
	String[] getSqlAlterStrings(
			Table table,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext context);
}
