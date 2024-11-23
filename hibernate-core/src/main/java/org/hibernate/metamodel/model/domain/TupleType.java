/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;

import org.hibernate.query.sqm.SqmExpressible;

/**
 * Describes any structural type without a direct java type representation.
 *
 * @author Christian Beikov
 */
public interface TupleType<J> extends SqmExpressible<J> {

	int componentCount();
	String getComponentName(int index);
	List<String> getComponentNames();

	SqmExpressible<?> get(int index);
	SqmExpressible<?> get(String componentName);
}
