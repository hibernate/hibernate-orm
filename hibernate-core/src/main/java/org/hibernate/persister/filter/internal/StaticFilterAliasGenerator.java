/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


import org.hibernate.persister.filter.FilterAliasGenerator;

/**
 *
 * @author Rob Worsnop
 *
 */
public class StaticFilterAliasGenerator implements FilterAliasGenerator {

	private final String alias;

	public StaticFilterAliasGenerator(@Nonnull String alias) {
		this.alias = alias;
	}

	@Nullable
	@Override
	public String getAlias(@Nullable String table) {
		return alias;
	}

}
