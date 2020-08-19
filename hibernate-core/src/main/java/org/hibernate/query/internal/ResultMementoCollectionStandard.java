/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.ModelPartResultMementoCollection;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.complete.CompleteResultBuilderCollectionStandard;

/**
 * @author Steve Ebersole
 */
public class ResultMementoCollectionStandard implements ModelPartResultMementoCollection {
	private final NavigablePath navigablePath;
	private final PluralAttributeMapping pluralAttributeDescriptor;

	public ResultMementoCollectionStandard(
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeDescriptor) {
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
				navigablePath,
				pluralAttributeDescriptor
		);
	}
}
