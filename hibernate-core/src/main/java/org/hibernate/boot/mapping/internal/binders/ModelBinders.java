/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;

/// Composition root for reusable binders used while processing the domain model.
///
/// Most binders are created per type or per attribute because they carry local
/// source context.  `ModelBinders` owns the longer-lived collaborators that can be
/// shared safely across those short-lived binders, such as table binding support.
///
/// @since 9.0
/// @author Steve Ebersole
public class ModelBinders {

	// todo : keep this?

	private final TableBinder tableBinder;

	public ModelBinders(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.tableBinder = new TableBinder( bindingState, bindingOptions, bindingContext, this );
	}

	/// Binder for table references and mapping-model `Table` creation.
	public TableBinder getTableBinder() {
		return tableBinder;
	}
}
