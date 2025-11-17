/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import java.util.IdentityHashMap;

public class SimpleSqmRenderContext implements SqmRenderContext {

	private final IdentityHashMap<SqmFrom<?, ?>, String> fromAliases = new IdentityHashMap<>();
	private final IdentityHashMap<JpaCriteriaParameter<?>, String> parameterNames = new IdentityHashMap<>();
	private int fromId;
	private int parameterId;

	public SimpleSqmRenderContext() {
	}

	@Override
	public String resolveAlias(SqmFrom<?, ?> from) {
		final String explicitAlias = from.getExplicitAlias();
		return explicitAlias == null
				? fromAliases.computeIfAbsent( from, f -> "alias_" + (fromId++) )
				: explicitAlias;
	}

	@Override
	public String resolveParameterName(JpaCriteriaParameter<?> parameter) {
		return parameterNames.computeIfAbsent( parameter, p -> "__param_" + (parameterId++) );
	}
}
