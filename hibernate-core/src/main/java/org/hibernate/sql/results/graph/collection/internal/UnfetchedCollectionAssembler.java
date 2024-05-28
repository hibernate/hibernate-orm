/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

public class UnfetchedCollectionAssembler implements DomainResultAssembler {

	private final PluralAttributeMapping fetchedMapping;

	public UnfetchedCollectionAssembler(PluralAttributeMapping fetchedMapping) {
		this.fetchedMapping = fetchedMapping;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState) {
		return LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	@Override
	public JavaType getAssembledJavaType() {
		return fetchedMapping.getJavaType();
	}

}
