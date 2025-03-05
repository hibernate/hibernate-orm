/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderEntityValuedModelPart;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Christian Beikov
 */
public class FetchMementoEntityStandard implements FetchMemento {
	private final NavigablePath navigablePath;
	private final EntityValuedFetchable attributeMapping;
	private final List<String> columnNames;

	public FetchMementoEntityStandard(
			NavigablePath navigablePath,
			EntityValuedFetchable attributeMapping,
			List<String> columnNames) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
		this.columnNames = columnNames;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteFetchBuilderEntityValuedModelPart( navigablePath, attributeMapping, columnNames );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

}
