/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ImplicitAttributeFetchBuilder;

/**
 * @author Steve Ebersole
 */
public class ImplicitAttributeFetchMemento implements FetchMemento {
	private final NavigablePath navigablePath;
	private final AttributeMapping attributeMapping;

	public ImplicitAttributeFetchMemento(NavigablePath navigablePath, AttributeMapping attributeMapping) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new ImplicitAttributeFetchBuilder( navigablePath, attributeMapping );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

}
