/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultBuilderBasicValued;

/**
 * @author Steve Ebersole
 */
public interface ResultMementoBasic extends ResultMemento {
	@Override
	ResultBuilderBasicValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
