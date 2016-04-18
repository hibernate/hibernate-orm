/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Represents a 2-phase JPA bootstrap process for building a Hibernate EntityManagerFactory.
 *
 * The first phase is the process of instantiating this builder.  During the first phase, loading of Class references
 * is highly discouraged.
 *
 * The second phase is building the EntityManagerFactory instance via {@link #build}.
 *
 * If anything goes wrong during either phase and the bootstrap process needs to be aborted, {@link #cancel()} should
 * be called.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public interface EntityManagerFactoryBuilder {
	/**
	 * Allows passing in a Java EE ValidatorFactory (delayed from constructing the builder, AKA phase 2) to be used
	 * in building the EntityManagerFactory
	 *
	 * @param validatorFactory The ValidatorFactory
	 *
	 * @return {@code this}, for method chaining
	 */
	public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory);

	/**
	 * Allows passing in a DataSource (delayed from constructing the builder, AKA phase 2) to be used
	 * in building the EntityManagerFactory
	 *
	 * @param dataSource The DataSource to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public EntityManagerFactoryBuilder withDataSource(DataSource dataSource);

	/**
	 * Build {@link EntityManagerFactory} instance
	 *
	 * @return The built {@link EntityManagerFactory}
	 */
	public EntityManagerFactory build();

	/**
	 * Cancel the building processing.  This is used to signal the builder to release any resources in the case of
	 * something having gone wrong during the bootstrap process
	 */
	public void cancel();

	/**
	 * Perform an explicit schema generation (rather than an "auto" one) based on the
	 */
	public void generateSchema();
}
