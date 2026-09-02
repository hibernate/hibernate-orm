/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unit.sequence;

import org.hibernate.community.dialect.DerbyLegacyDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.unit.sequence.AbstractSequenceInformationExtractorTest;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11470")
public class DerbyTenSevenDialectSequenceInformationExtractorTest extends AbstractSequenceInformationExtractorTest {
	@Override
	public Dialect getDialect() {
		return new DerbyLegacyDialect( DatabaseVersion.make( 10, 7 ) );
	}

	@Override
	public boolean expectsSequenceMetadata() {
		return true;
	}

	@Override
	public String expectedQuerySequencesString() {
		return "select sys.sysschemas.schemaname as sequence_schema,sys.syssequences.* from sys.syssequences left join sys.sysschemas on sys.syssequences.schemaid=sys.sysschemas.schemaid";
	}
}
