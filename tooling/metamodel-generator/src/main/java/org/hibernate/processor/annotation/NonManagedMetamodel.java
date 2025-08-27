/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;

import javax.lang.model.element.TypeElement;

public class NonManagedMetamodel extends AnnotationMetaEntity {

	public NonManagedMetamodel(TypeElement element, Context context, boolean jakartaDataStaticMetamodel, @Nullable AnnotationMeta parent) {
		super( element, context, false, jakartaDataStaticMetamodel, parent, null );
	}

	public static NonManagedMetamodel create(
			TypeElement element, Context context,
			boolean jakartaDataStaticMetamodel,
			@Nullable AnnotationMetaEntity parent) {
		final NonManagedMetamodel metamodel =
				new NonManagedMetamodel( element, context, jakartaDataStaticMetamodel, parent );
		if ( parent != null ) {
			parent.addInnerClass( metamodel );
		}
		return metamodel;
	}

	protected void init() {
		// Initialization is not needed when non-managed class
	}

	@Override
	public String javadoc() {
		return "";
	}
}
