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

	JdbcValuesSourceProcessingOptions NO_OPTIONS =
			new JdbcValuesSourceProcessingOptions() {
				@Override
				public Object getEffectiveOptionalObject() {
					return null;
				}

				@Override
				public String getEffectiveOptionalEntityName() {
					return null;
				}

				@Override
				public Object getEffectiveOptionalId() {
					return null;
				}

				@Override
				public boolean shouldReturnProxies() {
					return true;
				}
			};
}
