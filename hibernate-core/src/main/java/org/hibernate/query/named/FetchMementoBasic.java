/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface FetchMementoBasic extends FetchMemento {
	@Override
	FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
