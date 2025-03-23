/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import java.util.function.Function;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * An exporter for temporary tables.
 * <p>
 * Unlike other {@linkplain org.hibernate.tool.schema.spi.Exporter DDL exporters},
 * this exporter is called at runtime, instead of during schema management.
 *
 * @author Steve Ebersole
 */
public interface TemporaryTableExporter {
	String getSqlCreateCommand(TemporaryTable idTable);

	String getSqlDropCommand(TemporaryTable idTable);

	String getSqlTruncateCommand(
			TemporaryTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session);
}
