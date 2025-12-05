/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

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
