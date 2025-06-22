/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderEmbeddableValuedModelPart;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class FetchMementoEmbeddableStandard implements FetchMemento {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart attributeMapping;
	private final List<String> columnNames;

	public FetchMementoEmbeddableStandard(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart attributeMapping,
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
		return new CompleteFetchBuilderEmbeddableValuedModelPart( navigablePath, attributeMapping, columnNames );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	public EmbeddableValuedModelPart getAttributeMapping() {
		return attributeMapping;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}
}
