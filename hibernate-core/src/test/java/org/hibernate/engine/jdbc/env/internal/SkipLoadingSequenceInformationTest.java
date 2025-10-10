/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * Verifies that setting {@code AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY} to {@code none}
 * is going to skip loading the sequence information from the database.
 */
@RequiresDialect( H2Dialect.class )
@JiraKey( value = "HHH-14667")
@DomainModel(annotatedClasses = {SkipLoadingSequenceInformationTest.SequencingEntity.class})
@SessionFactory
@ServiceRegistry(
		settings = { @Setting( name = AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, value = "NONE") },
		settingProviders = { @SettingProvider( settingName = Environment.DIALECT, provider = SkipLoadingSequenceInformationTest.VetoingDialectClassProvider.class) }
)
public class SkipLoadingSequenceInformationTest {

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

	public static class VetoingDialectClassProvider implements SettingProvider.Provider<Class<?>> {
		@Override
		public Class<?> getSetting() {
			return VetoingDialect.class;
		}
	}

}
