/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.sequence;

import org.hibernate.dialect.DB2390Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11470")
public class DB2390SequenceInformationExtractorTest extends AbstractSequenceInformationExtractorTest {

	@Override
	public Dialect getDialect() {
		return new DB2390Dialect();
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
