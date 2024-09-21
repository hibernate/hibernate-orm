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
public interface FilterAliasGenerator {
	String getAlias(String table);
}
