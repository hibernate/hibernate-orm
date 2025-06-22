/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.ModelPartResultMementoCollection;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderCollectionStandard;

/**
 * @author Steve Ebersole
 */
public class ResultMementoCollectionStandard implements ModelPartResultMementoCollection {
	private final String tableAlias;
	private final NavigablePath navigablePath;
	private final PluralAttributeMapping pluralAttributeDescriptor;

	public ResultMementoCollectionStandard(
			String tableAlias,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeDescriptor) {
		this.tableAlias = tableAlias;
		this.navigablePath = navigablePath;
		this.pluralAttributeDescriptor = pluralAttributeDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public PluralAttributeMapping getPluralAttributeDescriptor() {
		return pluralAttributeDescriptor;
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteResultBuilderCollectionStandard(
				tableAlias,
				navigablePath,
				pluralAttributeDescriptor
		);
	}
}
