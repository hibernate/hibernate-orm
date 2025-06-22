/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A {@link org.hibernate.generator.Generator} that supports "configuration".
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Configurable {
	/**
	 * @deprecated Use {@link #configure(GeneratorCreationContext, Properties)} instead
	 */
	@Deprecated( since = "7.0", forRemoval = true )
	default void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
	}

	/**
	 * Configure this instance, given the value of parameters
	 * specified by the user as XML {@code <param>} elements and
	 * {@link org.hibernate.annotations.Parameter @Parameter}
	 * annotations.
	 * <p>
	 * This method is called just once, following instantiation.
	 * If this instance also implements {@code ExportableProducer},
	 * then this method is always called before
	 * {@link org.hibernate.boot.model.relational.ExportableProducer#registerExportables(Database)},
	 *
	 * @param creationContext Access to the generator creation context
	 * @param parameters param values, keyed by parameter name
	 *
	 * @throws MappingException when there's something wrong with the given parameters
	 */
	default void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		configure( creationContext.getType(), parameters, creationContext.getServiceRegistry() );
	}

	/**
	 * Initializes this instance, pre-generating SQL if necessary.
	 * <p>
	 * If this instance also implements {@code ExportableProducer},
	 * then this method is always called after
	 * {@link org.hibernate.boot.model.relational.ExportableProducer#registerExportables(Database)},
	 * and before first use.
	 *
	 * @param context A context to help generate SQL strings
	 */
	default void initialize(SqlStringGenerationContext context) {}
}
