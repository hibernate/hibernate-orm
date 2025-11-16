/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter.internal;


import org.hibernate.persister.filter.FilterAliasGenerator;

/**
 *
 * @author Rob Worsnop
 *
 */
public class StaticFilterAliasGenerator implements FilterAliasGenerator {

	private final String alias;

	public StaticFilterAliasGenerator(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias(String table) {
		return alias;
	}

}
