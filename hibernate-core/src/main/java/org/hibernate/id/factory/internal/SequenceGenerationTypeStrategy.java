/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory.internal;

import java.util.Properties;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.GenerationType;

public class SequenceGenerationTypeStrategy implements GenerationTypeStrategy {
	/**
	 * Singleton access
	 */
	public static final SequenceGenerationTypeStrategy INSTANCE = new SequenceGenerationTypeStrategy();

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			GenerationType generationType,
			String generatorName,
			JavaType<?> javaType,
			Properties config,
			GeneratorDefinitionResolver definitionResolver,
			ServiceRegistry serviceRegistry) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
