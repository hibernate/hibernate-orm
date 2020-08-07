/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.complete.CompleteFetchBuilderBasicPart;

/**
 * Memento describing a basic-valued fetch.  A basic-value cannot be
 * de-referenced.
 *
 * @author Steve Ebersole
 */
public class BasicFetchMemento implements FetchMappingMemento {
	private final BasicValuedModelPart fetchedAttribute;
	private final String columnAlias;

	public BasicFetchMemento(BasicValuedModelPart fetchedAttribute, String columnAlias) {
		this.fetchedAttribute = fetchedAttribute;
		this.columnAlias = columnAlias;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteFetchBuilderBasicPart( fetchedAttribute, columnAlias );
	}
}
