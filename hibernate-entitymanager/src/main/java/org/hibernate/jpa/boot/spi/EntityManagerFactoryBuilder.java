/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
 * be called to release resources held between the 2 phases.
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
