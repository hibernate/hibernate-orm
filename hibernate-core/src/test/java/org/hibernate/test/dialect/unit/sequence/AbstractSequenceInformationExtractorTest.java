/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.sequence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractSequenceInformationExtractorTest {

	@Test
	public void testSequenceGenerationExtractor() {
		final Dialect dialect = getDialect();
		assertThat(
				dialect.getQuerySequencesString(),
				is( expectedQuerySequencesString() )
		);
		assertThat(
				dialect.getSequenceInformationExtractor(),
				instanceOf( expectedSequenceInformationExtractor() )
		);
	}

	public abstract Dialect getDialect();

	public abstract String expectedQuerySequencesString();

	public abstract Class<? extends SequenceInformationExtractor> expectedSequenceInformationExtractor();

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		public Integer id;
	}
}
