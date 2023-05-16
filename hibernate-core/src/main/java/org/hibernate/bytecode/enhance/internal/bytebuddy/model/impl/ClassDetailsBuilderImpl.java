/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetailsBuilder;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelProcessingContext;

import net.bytebuddy.description.type.TypeDescription;

/**
 * ClassDetailsBuilder implementation
 *
 * @author Steve Ebersole
 */
public class ClassDetailsBuilderImpl implements ClassDetailsBuilder {
	private final ModelProcessingContext modelProcessingContext;

	public ClassDetailsBuilderImpl(ModelProcessingContextImpl modelProcessingContext) {
		this.modelProcessingContext = modelProcessingContext;
	}

	@Override
	public ClassDetails buildClassDetails(String name, ModelProcessingContext processingContext) {
		final TypeDescription typeDescription = modelProcessingContext.getTypePool().describe( name ).resolve();
		return buildClassDetails( name, typeDescription, processingContext );
	}

	public ClassDetails buildClassDetails(String name, TypeDescription typeDescription, ModelProcessingContext processingContext) {
		return new ClassDetailsImpl( name, typeDescription, processingContext );
	}
}
