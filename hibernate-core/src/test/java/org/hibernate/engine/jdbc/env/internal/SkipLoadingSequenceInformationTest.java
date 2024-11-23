/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Verifies that setting {@code AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY} to {@code none}
 * is going to skip loading the sequence information from the database.
 */
@TestForIssue( jiraKey = "HHH-14667")
public class SkipLoadingSequenceInformationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.NONE.name() );
		configuration.setProperty( Environment.DIALECT, VetoingDialect.class.getName() );
	}

	@Entity(name="seqentity")
	static class SequencingEntity {
		@Id
		@GenericGenerator(name = "pooledoptimizer", strategy = "enhanced-sequence",
				parameters = {
						@org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
						@org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
						@org.hibernate.annotations.Parameter(name = "increment_size", value = "2")
				}
		)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pooledoptimizer")
		Integer id;
		String name;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{SequencingEntity.class};
	}

	@Test
	public void test() {
		// If it's able to boot, we're good.
	}

	public static class VetoingDialect extends H2Dialect {
		@Override
		public SequenceInformationExtractor getSequenceInformationExtractor() {
			throw new IllegalStateException("Should really not invoke this method in this setup");
		}
	}

}
