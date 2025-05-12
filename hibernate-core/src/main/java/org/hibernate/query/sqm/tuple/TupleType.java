/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.SqmBindable;

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

	SqmBindable<?> get(int index);
	SqmBindable<?> get(String componentName);
}
