/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory;

import java.util.Properties;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.generator.Generator;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.GenerationType;

/**
 * Contract for a factory of {@link IdentifierGenerator} instances. The implementor
 * of this service is responsible for providing implementations of the predefined
 * built-in id generators, all of which implement {@link IdentifierGenerator}, along
 * with any id generator declared using {@link org.hibernate.annotations.GenericGenerator}.
 * <p>
 * An id generator is identified by either:
 * <ul>
 * <li>a predefined string-based name (which originated in the old {@code hbm.xml}
 *     mapping file format),
 * <li>the {@link org.hibernate.annotations.GenericGenerator#name name} or the
 *     the {@link org.hibernate.annotations.GenericGenerator#type type} specified
 *     by the {@code @GenericGenerator} annotation, or
 * <li>a JPA-defined {@link GenerationType}.
 * </ul>
 * <p>
 * A new generator passed a {@link Properties} object containing parameters via the
 * method {@link IdentifierGenerator#configure(Type, Properties, ServiceRegistry)}.
 * <p>
 * This is part of an older mechanism for instantiating and configuring id generators
 * which predates the existence of {@link Generator} and the
 * {@link org.hibernate.annotations.IdGeneratorType @IdGeneratorType} meta-annotation.
 *
 * @author Steve Ebersole
 */
@Incubating //this API is currently in flux
public interface IdentifierGeneratorFactory extends Service {
	/**
	 * Create an {@link IdentifierGenerator} based on the given details.
	 */
	@Incubating
	Generator createIdentifierGenerator(
			GenerationType generationType,
			String generatedValueGeneratorName,
			String generatorName,
			JavaType<?> javaType,
			Properties config,
			GeneratorDefinitionResolver definitionResolver);

	/**
	 * Given a strategy, retrieve the appropriate identifier generator instance.
	 *
	 * @param strategy The generation strategy.
	 * @param type The mapping type for the identifier values.
	 * @param parameters Any parameters properties given in the generator mapping.
	 *
	 * @return The appropriate generator instance.
	 *
	 * @deprecated use {@link #createIdentifierGenerator(GenerationType, String, String, JavaType, Properties, GeneratorDefinitionResolver)}
	 */
	@Deprecated(since = "6.0")
	Generator createIdentifierGenerator(String strategy, Type type, Properties parameters);
}
