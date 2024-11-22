/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;

import javax.lang.model.element.TypeElement;

public class NonManagedMetamodel extends AnnotationMetaEntity {

	public NonManagedMetamodel(TypeElement element, Context context, boolean jakartaDataStaticMetamodel, @Nullable AnnotationMeta parent) {
		super( element, context, false, jakartaDataStaticMetamodel, parent );
	}

	public static NonManagedMetamodel create(
			TypeElement element, Context context,
			boolean jakartaDataStaticMetamodel,
			@Nullable AnnotationMetaEntity parent) {
		final NonManagedMetamodel metamodel =
				new NonManagedMetamodel( element, context, jakartaDataStaticMetamodel, parent );
		if ( parent != null ) {
			metamodel.setParentElement( parent.getElement() );
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
