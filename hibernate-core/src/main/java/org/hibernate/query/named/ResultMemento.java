/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultBuilder;

/**
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultMemento extends ResultMappingMementoNode {
	ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);
}
