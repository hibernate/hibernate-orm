/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

/**
 * Essentially processing options only for entity loading
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesSourceProcessingOptions {
	Object getEffectiveOptionalObject();
	String getEffectiveOptionalEntityName();
	Object getEffectiveOptionalId();

	boolean shouldReturnProxies();
}
