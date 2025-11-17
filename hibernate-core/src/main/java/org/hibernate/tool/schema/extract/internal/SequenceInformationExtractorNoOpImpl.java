/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.SQLException;

import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class SequenceInformationExtractorNoOpImpl implements SequenceInformationExtractor {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorNoOpImpl INSTANCE = new SequenceInformationExtractorNoOpImpl();

	@Override
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		return emptyList();
	}
}
