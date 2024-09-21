/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.dynamic;

import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilder extends FetchBuilder, NativeQuery.ReturnProperty {
	DynamicFetchBuilder cacheKeyInstance();

	List<String> getColumnAliases();
}
