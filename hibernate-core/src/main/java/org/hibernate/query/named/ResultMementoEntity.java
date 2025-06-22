/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultBuilderEntityValued;

/**
 * @author Steve Ebersole
 */
public interface ResultMementoEntity extends ResultMemento {
	@Override
	ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
