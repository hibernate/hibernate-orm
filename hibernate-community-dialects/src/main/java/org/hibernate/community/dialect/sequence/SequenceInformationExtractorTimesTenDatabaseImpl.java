/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorTimesTenDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorTimesTenDatabaseImpl INSTANCE = new SequenceInformationExtractorTimesTenDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "name";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return null;
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "minval";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "maxval";
	}
}
