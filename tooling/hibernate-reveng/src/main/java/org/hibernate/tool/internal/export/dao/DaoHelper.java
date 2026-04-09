/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2020-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;

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
