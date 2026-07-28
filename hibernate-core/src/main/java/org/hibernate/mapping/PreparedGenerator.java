/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Internal;
import org.hibernate.generator.Generator;
import org.hibernate.resource.beans.spi.ManagedBean;

/// A live generator prepared for one metadata/schema/SessionFactory
/// construction flow.
///
/// This is the non-serializable counterpart to [GeneratorDescriptor]. A
/// descriptor contains archive-safe instructions; a `PreparedGenerator`
/// contains the live managed reference produced by executing those
/// instructions.
///
/// A prepared product may be created while the relational model is finalized
/// when the generator implements `ExportableProducer` or otherwise requires
/// early access. In that case it is stored in transient metadata state and
/// consumed once by SessionFactory construction. This handoff guarantees that
/// the generator which was configured, initialized, and asked to register
/// exportables is the generator installed in the runtime model. A
/// non-exportable generator is usually prepared directly during SessionFactory
/// construction instead.
///
/// The `ManagedBean` is retained, rather than just its current bean instance,
/// so the managed contextual reference remains associated with the
/// construction flow. Hibernate's managed-bean lookup stabilizes that
/// reference before constructing this product, and so repeated
/// [#getGenerator()] calls return the same prepared instance.
///
/// `PreparedGenerator` performs no creation or lifecycle callbacks itself.
/// In particular, it does not configure, initialize, register exportables, or
/// destroy the generator. Those preparation callbacks occur before this
/// product is returned by
/// [GeneratorDescriptor#prepareGenerator(org.hibernate.generator.GeneratorCreationContext)].
///
/// @param managedBean the stable managed reference for the prepared generator
///
/// @implNote Unlike [GeneratorDescriptor], this contract is deliberately not
/// serializable and must never be retained as archived mapping state. The
/// transient prepared-generator registries on metadata are a construction-time
/// handoff, not part of the reusable boot model.
///
/// @author Steve Ebersole
/// @since 9.0
@Internal
public record PreparedGenerator<G extends Generator>(ManagedBean<G> managedBean) {

	/// Obtain the completely prepared generator instance.
	///
	/// This method only dereferences the retained managed reference; it does
	/// not perform any generator lifecycle callback.
	///
	/// @return the prepared generator instance
	public G getGenerator() {
		return managedBean.getBeanInstance();
	}

	/// Wrap an already-created, unmanaged generator in a direct managed
	/// reference.
	///
	/// This is used by descriptor compatibility paths which create the
	/// generator directly. The resulting product has stable identity but no
	/// container-managed lifecycle.
	///
	/// @param generator an already-created and prepared generator
	/// @return a prepared product retaining that exact instance
	public static <G extends Generator> PreparedGenerator<G> fromGenerator(G generator) {
		return new PreparedGenerator<>( new DirectManagedBean<>( generator ) );
	}

	private record DirectManagedBean<G extends Generator>(G generator) implements ManagedBean<G> {
		@Override
		@SuppressWarnings("unchecked")
		public Class<G> getBeanClass() {
			return (Class<G>) generator.getClass();
		}

		@Override
		public G getBeanInstance() {
			return generator;
		}
	}
}
