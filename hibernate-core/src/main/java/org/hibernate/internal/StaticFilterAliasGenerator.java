/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;


/**
 *
 * @author Rob Worsnop
 *
 */
public class StaticFilterAliasGenerator implements FilterAliasGenerator{

	private final String alias;

	public StaticFilterAliasGenerator(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias(String table) {
		return alias;
	}

}
