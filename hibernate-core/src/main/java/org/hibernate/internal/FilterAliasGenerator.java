/*
 * SPDX-License-Identifier: Apache-2.0
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
