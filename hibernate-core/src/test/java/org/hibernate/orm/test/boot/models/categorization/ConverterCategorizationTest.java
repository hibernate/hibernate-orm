/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorization;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.spi.ConverterRegistration;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * Tests that {@link DomainModelCategorizationCollector} correctly identifies
 * converter classes and does not treat the {@link AttributeConverter} interface
 * itself as a converter.
 */
public class ConverterCategorizationTest {

	@Test
	void attributeConverterInterfaceIsNotCategorizedAsConverter() {
		// Build a context that includes both the converter implementation
		// and the AttributeConverter interface itself
		final ModelsContext modelsContext = createBuildingContext(
				TestConverter.class,
				AttributeConverter.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			// Apply all known classes, including AttributeConverter itself
			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			final Set<ConverterRegistration> converters = globalRegistrations.getJpaConverters();

			// Only the concrete converter should be registered, not the interface
			assertThat( converters )
					.extracting( reg -> reg.converterClass().getClassName() )
					.containsExactly( TestConverter.class.getName() );
		}
	}

	@Test
	void abstractConverterIsNotCategorizedAsConverter() {
		final ModelsContext modelsContext = createBuildingContext(
				TestConverter.class,
				AbstractBaseConverter.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			final Set<ConverterRegistration> converters = globalRegistrations.getJpaConverters();

			assertThat( converters )
					.extracting( reg -> reg.converterClass().getClassName() )
					.containsExactly( TestConverter.class.getName() );
		}
	}

	public static abstract class AbstractBaseConverter implements AttributeConverter<String, String> {
	}

	@Converter(autoApply = true)
	public static class TestConverter extends AbstractBaseConverter {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}
}
