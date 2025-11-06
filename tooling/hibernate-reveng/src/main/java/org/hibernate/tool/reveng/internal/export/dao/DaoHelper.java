/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.dao;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DaoHelper {

	public static Iterable<NamedHqlQueryDefinition<?>> getNamedHqlQueryDefinitions(Metadata metadata) {
		List<NamedHqlQueryDefinition<?>> result = new ArrayList<NamedHqlQueryDefinition<?>>();
		metadata.visitNamedHqlQueryDefinitions(new Consumer<NamedHqlQueryDefinition<?>>() {
			@Override
			public void accept(NamedHqlQueryDefinition<?> t) {
				result.add(t);
			}
		});
		return result;
	}

}
