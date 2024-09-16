/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.complete.CompleteFetchBuilderBasicPart;

/**
 * Memento describing a basic-valued fetch.  A basic-value cannot be
 * de-referenced.
 *
 * @author Steve Ebersole
 */
public class FetchMementoBasicStandard implements FetchMementoBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart fetchedAttribute;
	private final String columnAlias;

	public FetchMementoBasicStandard(
			NavigablePath navigablePath,
			BasicValuedModelPart fetchedAttribute,
			String columnAlias) {
		this.navigablePath = navigablePath;
		this.fetchedAttribute = fetchedAttribute;
		this.columnAlias = columnAlias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	public BasicValuedModelPart getFetchedAttribute() {
		return fetchedAttribute;
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteFetchBuilderBasicPart( navigablePath, fetchedAttribute, columnAlias );
	}
}
