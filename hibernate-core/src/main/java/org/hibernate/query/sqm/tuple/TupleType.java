/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.SqmBindableType;

import java.util.List;

/**
 * Describes any structural type without a direct java type representation.
 *
 * @author Christian Beikov
 */
public interface TupleType<J> extends ReturnableType<J> {

	int componentCount();
	String getComponentName(int index);
	List<String> getComponentNames();

	SqmBindableType<?> get(int index);
	@Nullable SqmBindableType<?> get(String componentName);
}
