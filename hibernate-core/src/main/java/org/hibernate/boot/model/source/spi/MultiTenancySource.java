/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source information related to mapping the multi-tenancy of an entity
 *
 * @author Steve Ebersole
 */
public interface MultiTenancySource {
	/**
	 * Obtain the column/formula information about the multi-tenancy discriminator.
	 *
	 * @return The column/formula information
	 */
	RelationalValueSource getRelationalValueSource();

	boolean isShared();

	boolean bindAsParameter();
}
