/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.ModelPartResultMemento;
import org.hibernate.query.results.internal.implicit.ImplicitModelPartResultBuilderBasic;
import org.hibernate.query.results.internal.implicit.ImplicitModelPartResultBuilderEmbeddable;
import org.hibernate.query.results.internal.implicit.ImplicitModelPartResultBuilderEntity;
import org.hibernate.query.results.ResultBuilder;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultMemento implements ModelPartResultMemento {
	private final NavigablePath navigablePath;
	private final ModelPart referencedModelPart;

	public ImplicitModelPartResultMemento(NavigablePath navigablePath, ModelPart referencedModelPart) {
		this.navigablePath = navigablePath;
		this.referencedModelPart = referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final BasicValuedModelPart basicPart = referencedModelPart.asBasicValuedModelPart();
		if ( basicPart != null ) {
			return new ImplicitModelPartResultBuilderBasic( navigablePath, basicPart );
		}

		if ( referencedModelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			// todo (6.0) : can this really happen?
			return new ImplicitModelPartResultBuilderEmbeddable( navigablePath, embeddableValuedModelPart );
		}

		if ( referencedModelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			return new ImplicitModelPartResultBuilderEntity( navigablePath, entityValuedModelPart );
		}

		throw new IllegalStateException( "Unknown type of model part : "+ referencedModelPart );
	}
}
