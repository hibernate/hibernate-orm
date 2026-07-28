/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;


/**
 * Handles interpretation of old {@code hbm.xml}-style generator strategy names.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public final class GeneratorStrategies {
	private GeneratorStrategies() {
	}

	/**
	 * Interpret an "old" generator strategy name as a {@link Generator} class.
	 */
	public static Class<? extends Generator> resolveGeneratorClass(
			String strategy,
			MetadataBuildingContext context) {
		return resolveGeneratorClass(
				strategy,
				context.getMetadataCollector().getDatabase().getDialect(),
				context.getClassLoaderService()
		);
	}

	/**
	 * Interpret an "old" generator strategy name as a {@link Generator} class.
	 */
	public static Class<? extends Generator> resolveGeneratorClass(
			String strategy,
			Dialect dialect,
			ClassLoaderService classLoaderService) {
		final Class<? extends Generator> legacyNamedGenerator = legacyGeneratorClass( strategy, dialect );
		if ( legacyNamedGenerator != null ) {
			return legacyNamedGenerator;
		}
		final Class<? extends Generator> clazz = classLoaderService.classForName( strategy );
		if ( !Generator.class.isAssignableFrom( clazz ) ) {
			// in principle, this shouldn't happen, since @GenericGenerator
			// constrains the type to subtypes of Generator
			throw new MappingException( clazz.getName() + " does not implement 'Generator'" );
		}
		return clazz;
	}

	private static Class<? extends Generator> legacyGeneratorClass(String strategy, Dialect dialect) {
		if ( "native".equals(strategy) ) {
			strategy = dialect.getNativeIdentifierGeneratorStrategy();
		}
		switch (strategy) {
			case "assigned":
				return org.hibernate.id.Assigned.class;
			case "enhanced-sequence":
			case "sequence":
				return SequenceStyleGenerator.class;
			case "enhanced-table":
			case "table":
				return org.hibernate.id.enhanced.TableGenerator.class;
			case "identity":
				return IdentityGenerator.class;
			case "increment":
				return IncrementGenerator.class;
			case "select":
				return SelectGenerator.class;
		}

		return null;
	}

	static Class<? extends Generator> resolveLegacyGeneratorClass(
			String strategy,
			MetadataBuildingContext buildingContext) {
		return legacyGeneratorClass(
				strategy,
				buildingContext.getMetadataCollector().getDatabase().getDialect()
		);
	}
}
