/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.ModelPartResultMemento;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderBasic;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderEmbeddable;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderEntity;
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

		if ( referencedModelPart instanceof EmbeddableValuedModelPart ) {
			// todo (6.0) : can this really happen?
			return new ImplicitModelPartResultBuilderEmbeddable( navigablePath, (EmbeddableValuedModelPart) referencedModelPart );
		}

		if ( referencedModelPart instanceof EntityValuedModelPart ) {
			return new ImplicitModelPartResultBuilderEntity( navigablePath, (EntityValuedModelPart) referencedModelPart );
		}

		throw new IllegalStateException( "Unknown type of model part : "+ referencedModelPart );
	}
}
