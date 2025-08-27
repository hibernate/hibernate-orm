/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unit.sequence;

import org.hibernate.community.dialect.DB2iLegacyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.unit.sequence.AbstractSequenceInformationExtractorTest;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.orm.junit.JiraKey;


/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11470")
public class DB2400SequenceInformationExtractorTest extends AbstractSequenceInformationExtractorTest {

	@Override
	public Dialect getDialect() {
		return new DB2iLegacyDialect();
	}

	@Override
	public String expectedQuerySequencesString() {
		return null;
	}

	@Override
	public Class<? extends SequenceInformationExtractor> expectedSequenceInformationExtractor() {
		return SequenceInformationExtractorNoOpImpl.class;
	}
}
