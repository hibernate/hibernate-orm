/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.GenerationType;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.TypeDetails;

import java.util.UUID;

/**
 * Handles interpretation of old {@code hbm.xml}-style generator strategy names.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public class GeneratorStrategies {

	/**
	 * Interpret a JPA {@link GenerationType} as an old-style generator strategy name.
	 *
	 * @param generationType the type specified by {@link jakarta.persistence.GeneratedValue#strategy}
	 * @param name           the name specified by {@link jakarta.persistence.GeneratedValue#generator}
	 * @param type           the Java type of the generated identifier
	 * @return a {@code hbml.xml}-equivalent string identifying the strategy
	 */
	public static String generatorStrategy(GenerationType generationType, String name, TypeDetails type) {
		switch ( generationType ) {
			case IDENTITY:
				return "identity";
			case SEQUENCE:
				return SequenceStyleGenerator.class.getName();
			case TABLE:
				return org.hibernate.id.enhanced.TableGenerator.class.getName();
			case UUID:
				return UUIDGenerator.class.getName();
			case AUTO:
				if ( UUID.class.isAssignableFrom( type.determineRawClass().toJavaClass() ) ) {
					return UUIDGenerator.class.getName();
				}
				else if ( "increment".equalsIgnoreCase( name ) ) {
					// special case for @GeneratedValue(name="increment")
					// for some reason we consider there to be an implicit
					// generator named 'increment' (doesn't seem very useful)
					return IncrementGenerator.class.getName();
				}
				else {
					return SequenceStyleGenerator.class.getName();
				}
			default:
				throw new IllegalArgumentException( "Unsupported generation type:" + generationType );
		}
	}

	/**
	 * Interpret an "old" generator strategy name as a {@link Generator} class.
	 */
	public static Class<? extends Generator> generatorClass(String strategy, SimpleValue idValue) {
		if ( "native".equals(strategy) ) {
			strategy =
					idValue.getMetadata().getDatabase().getDialect()
							.getNativeIdentifierGeneratorStrategy();
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
			case "foreign":
				return ForeignGenerator.class;
			case "uuid":
			case "uuid.hex":
				return UUIDHexGenerator.class;
			case "uuid2":
				return UUIDGenerator.class;
			case "select":
				return SelectGenerator.class;
			case "guid":
				return GUIDGenerator.class;
		}
		final Class<? extends Generator> clazz =
				idValue.getBuildingContext().getBootstrapContext()
						.getClassLoaderService()
						.classForName( strategy );
		if ( !Generator.class.isAssignableFrom( clazz ) ) {
			// in principle, this shouldn't happen, since @GenericGenerator
			// constrains the type to subtypes of Generator
			throw new MappingException( clazz.getName() + " does not implement 'Generator'" );
		}
		return clazz;
	}

	public static Class<? extends Generator> mapLegacyNamedGenerator(String strategy, Dialect dialect) {
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
			case "foreign":
				return ForeignGenerator.class;
			case "uuid":
			case "uuid.hex":
				return UUIDHexGenerator.class;
			case "uuid2":
				return UUIDGenerator.class;
			case "select":
				return SelectGenerator.class;
			case "guid":
				return GUIDGenerator.class;
		}

		return null;
	}

	public static Class<? extends Generator> mapLegacyNamedGenerator(String strategy, MetadataBuildingContext buildingContext) {
		return mapLegacyNamedGenerator( strategy, buildingContext.getMetadataCollector().getDatabase().getDialect() );
	}

	public static Class<? extends Generator> mapLegacyNamedGenerator(String strategy, SimpleValue idValue) {
		return mapLegacyNamedGenerator( strategy, idValue.getMetadata().getDatabase().getDialect() );
	}
}
