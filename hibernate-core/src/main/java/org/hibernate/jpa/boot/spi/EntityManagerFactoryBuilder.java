/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * Represents a two-phase JPA bootstrap process for building a Hibernate {@link EntityManagerFactory}.
 * <p>
 * The first phase is the process of instantiating this builder.  During the first phase, loading of
 * Class references is highly discouraged.
 * <p>
 * The second phase is building the {@code EntityManagerFactory} instance via {@link #build}.
 * <p>
 * If anything goes wrong during either phase and the bootstrap process needs to be aborted,
 * {@link #cancel()} should be called.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public interface EntityManagerFactoryBuilder {
	ManagedResources getManagedResources();

	MetadataImplementor metadata();

	/**
	 * Allows passing in a Java EE ValidatorFactory (delayed from constructing the builder, AKA phase 2) to be used
	 * in building the EntityManagerFactory
	 *
	 * @param validatorFactory The ValidatorFactory
	 *
	 * @return {@code this}, for method chaining
	 */
	EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory);

	/**
	 * Allows passing in a DataSource (delayed from constructing the builder, AKA phase 2) to be used
	 * in building the EntityManagerFactory
	 *
	 * @param dataSource The DataSource to use
	 *
	 * @return {@code this}, for method chaining
	 */
	EntityManagerFactoryBuilder withDataSource(DataSource dataSource);

	/**
	 * Build {@link EntityManagerFactory} instance
	 *
	 * @return The built {@link EntityManagerFactory}
	 */
	EntityManagerFactory build();

	/**
	 * Cancel the building processing.  This is used to signal the builder to release any resources in the case of
	 * something having gone wrong during the bootstrap process
	 */
	void cancel();

	/**
	 * Perform an explicit schema generation (rather than an "auto" one) based on the
	 */
	void generateSchema();
}
