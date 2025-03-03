/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.named.ResultMementoInstantiation;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderInstantiation;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class ResultMementoInstantiationStandard implements ResultMementoInstantiation {

	private final JavaType<?> instantiatedJtd;
	private final List<ArgumentMemento> argumentMementos;

	public ResultMementoInstantiationStandard(
			JavaType<?> instantiatedJtd,
			List<ArgumentMemento> argumentMementos) {
		this.instantiatedJtd = instantiatedJtd;
		this.argumentMementos = argumentMementos;
	}

	public JavaType<?> getInstantiatedJavaType() {
		return instantiatedJtd;
	}

	public List<ArgumentMemento> getArgumentMementos() {
		return Collections.unmodifiableList( argumentMementos );
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final List<ResultBuilder> argumentBuilders = CollectionHelper.arrayList( argumentMementos.size() );

		argumentMementos.forEach(
				argumentMemento -> argumentBuilders.add(
						argumentMemento.resolve( querySpaceConsumer, context )
				)
		);

		return new CompleteResultBuilderInstantiation( instantiatedJtd, argumentBuilders );
	}
}
