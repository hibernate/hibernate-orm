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
 * Models a SQL ResultSet mapping generally defined via {@linkplain jakarta.persistence.SqlResultSetMapping annotations}.
 * May also be created via
 * .
 *
 * [1] Or through
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultMemento extends ResultMappingMementoNode {

	Class<?> getResultJavaType();


	ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);
}
