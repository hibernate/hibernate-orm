/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import jakarta.annotation.Nonnull;

import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.spi.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilder extends FetchBuilder, NativeQuery.ReturnProperty {
	@Nonnull
	DynamicFetchBuilder cacheKeyInstance();

	@Nonnull
	List<String> getColumnAliases();
}
