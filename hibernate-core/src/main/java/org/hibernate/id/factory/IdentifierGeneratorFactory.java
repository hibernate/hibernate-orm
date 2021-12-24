/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.GenerationType;

/**
 * Contract for a {@code factory} of {@link IdentifierGenerator} instances.
 *
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorFactory {
	/**
	 * Get the dialect.
	 *
	 * @return the dialect
	 */
	Dialect getDialect();

	/**
	 * Create an IdentifierGenerator based on the given details
	 */
	IdentifierGenerator createIdentifierGenerator(
			GenerationType generationType,
			String generatedValueGeneratorName,
			String generatorName,
			JavaType<?> javaTypeDescriptor,
			Properties config,
			GeneratorDefinitionResolver definitionResolver);

	/**
	 * Given a strategy, retrieve the appropriate identifier generator instance.
	 *
	 * @param strategy The generation strategy.
	 * @param type The mapping type for the identifier values.
	 * @param config Any configuration properties given in the generator mapping.
	 *
	 * @return The appropriate generator instance.
	 *
	 * @deprecated (since 6.0) use {@link #createIdentifierGenerator(GenerationType, String, String, JavaType, Properties, GeneratorDefinitionResolver)}
	 * instead
	 */
	@Deprecated
	IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config);

	/**
	 * Retrieve the class that will be used as the {@link IdentifierGenerator} for the given strategy.
	 *
	 * @param strategy The strategy
	 * @return The generator class.
	 *
	 * @deprecated (since 6.0) with no replacement.  See
	 * {@link #createIdentifierGenerator(GenerationType, String, String, JavaType, Properties, GeneratorDefinitionResolver)}
	 */
	@Deprecated
	Class getIdentifierGeneratorClass(String strategy);
}
