/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory.spi;

import java.util.Properties;
import jakarta.persistence.GenerationType;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Delegate for defining how to handle the various types of
 * {@link GenerationType} possibilities.
 *
 * @apiNote no GenerationType indicates `hbm.xml` mapping
 */
public interface GenerationTypeStrategy {
	IdentifierGenerator createIdentifierGenerator(
			GenerationType generationType,
			String generatorName,
			JavaType<?> javaType,
			Properties config,
			GeneratorDefinitionResolver definitionResolver,
			ServiceRegistry serviceRegistry);
}
