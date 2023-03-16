/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory.spi;

import java.util.function.BiConsumer;
import jakarta.persistence.GenerationType;

import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.ServiceRegistry;

/**
 * A {@link java.util.ServiceLoader} contract for registering implementations
 * of {@link GenerationTypeStrategy}.
 */
@JavaServiceLoadable
public interface GenerationTypeStrategyRegistration {
	void registerStrategies(BiConsumer<GenerationType, GenerationTypeStrategy> registry, ServiceRegistry serviceRegistry);
}
