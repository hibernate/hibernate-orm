/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter;

import jakarta.annotation.Nullable;
/**
 *
 * @author Rob Worsnop
 *
 */
public interface FilterAliasGenerator {
	@Nullable
	String getAlias(@Nullable String table);
}
