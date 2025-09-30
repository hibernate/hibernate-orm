/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.tool.schema.extract.spi.ExtractionContext;

/**
 * @since 7.2
 */
public class InformationExtractorOracleImpl extends InformationExtractorJdbcDatabaseMetaDataImpl {

	public InformationExtractorOracleImpl(ExtractionContext extractionContext) {
		super( extractionContext );
	}

	@Override
	public boolean supportsBulkPrimaryKeyRetrieval() {
		return true;
	}

	@Override
	public boolean supportsBulkForeignKeyRetrieval() {
		return true;
	}

	// Unfortunately, there is no support for table wildcard for indexes
}
