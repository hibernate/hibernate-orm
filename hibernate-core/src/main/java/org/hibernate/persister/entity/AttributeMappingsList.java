/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity;

import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.sql.results.graph.Fetchable;

@Incubating
public interface AttributeMappingsList /*implements List<AttributeMapping> */ {

	int size();

	AttributeMapping getAttributeMapping(int i);

	void forEachFetchable(Consumer<? super Fetchable> fetchableConsumer);

	void forEachAttributeMapping(Consumer<? super AttributeMapping> attributeMappingConsumer);

	Fetchable getFetchable(int i);

	Iterable<AttributeMapping> iterateAsAttributeMappings();

	void forEachAttributeMapping(IndexedConsumer<AttributeMapping> consumer);

	void forEachFetchable(IndexedConsumer<Fetchable> consumer);

}
