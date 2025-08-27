/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.sequence;

import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11470")
@RequiresDialect(DB2zDialect.class)
public class DB2zSequenceInformationExtractorTest extends AbstractSequenceInformationExtractorTest {

	@Override
	public Dialect getDialect() {
		return new DB2zDialect();
	}

	@Override
	public String expectedQuerySequencesString() {
		return "select * from sysibm.syssequences";
	}

	@Override
	public Class<? extends SequenceInformationExtractor> expectedSequenceInformationExtractor() {
		return SequenceInformationExtractorDB2DatabaseImpl.class;
	}
}
