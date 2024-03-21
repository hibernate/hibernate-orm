/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Map;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.models.spi.ClassDetails;

/**
 * Binding for an {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
 *
 * @author Steve Ebersole
 */
public abstract class ManagedTypeBinding extends Binding {
	protected final ClassDetails classDetails;

	public ManagedTypeBinding(
			ClassDetails classDetails,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( bindingOptions, bindingState, bindingContext );
		this.classDetails = classDetails;
	}

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public abstract Map<String, AttributeBinding> getAttributeBindings();
}
