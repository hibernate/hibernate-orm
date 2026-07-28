/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;

/// Declarative, serializable boot-time description of a [Generator].
///
/// A descriptor belongs to the boot mapping model. It describes how to create
/// and prepare a generator, but does not retain a generator instance or a
/// managed-bean reference. Consequently, a descriptor may be retained in a
/// metadata archive and used in a later construction flow.
///
/// Generator handling proceeds through these phases:
///
/// 1. During mapping binding, the declaration is normalized into a descriptor.
/// 2. During relational-model finalization,
///    [#applyRelationalModel(GeneratorCreationContext)] applies any known
///    mapping semantics which do not require a live instance.
/// 3. If [#requiresBootPreparation(GeneratorCreationContext)] is `true`,
///    [#prepareGenerator(GeneratorCreationContext)] creates and prepares the
///    live generator. Exportable generators are therefore available while
///    schema objects are being collected.
/// 4. The resulting [PreparedGenerator] is retained only in transient
///    construction state and consumed by SessionFactory construction. The
///    exact generator instance used during export is handed to the runtime
///    model.
/// 5. If boot preparation is unnecessary, preparation is deferred until
///    SessionFactory construction.
///
/// A descriptor should expose its effective runtime class whenever possible.
/// The class is used to determine whether early preparation is required.
/// Descriptors which cannot expose it are treated conservatively as requiring
/// boot preparation.
///
/// Preparation is responsible for the complete one-time setup of the live
/// generator. Where the corresponding contracts apply, the expected callback
/// order is:
///
/// 1. obtain or instantiate the managed bean,
/// 2. perform annotation-based initialization,
/// 3. call `Configurable.configure()`,
/// 4. call `ExportableProducer.registerExportables()`, and
/// 5. call `Configurable.initialize()`.
///
/// Implementations which obtain generators through the managed-bean registry
/// must override [#prepareGenerator(GeneratorCreationContext)] so the
/// construction-scoped managed reference is retained in the returned product.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
@FunctionalInterface
public interface GeneratorDescriptor extends Serializable {
	/// Create and completely prepare a generator.
	///
	/// This is the compatibility creation entry point. Callers participating in
	/// bootstrap should normally use [#prepareGenerator(GeneratorCreationContext)]
	/// so a managed-bean reference is not discarded. Implementations which
	/// override `prepareGenerator()` commonly implement this method by
	/// delegating to it and obtaining the generator from the returned product.
	///
	/// This method and `prepareGenerator()` describe alternative entry points
	/// for one preparation operation; callers must not invoke both for the same
	/// usage site.
	///
	/// @param context the mapping and bootstrap services for this usage site
	/// @return the completely prepared generator
	Generator createGenerator(GeneratorCreationContext context);

	/// Create the live, construction-scoped generator product.
	///
	/// Descriptor implementations which obtain a generator from a managed bean
	/// container override this method to retain the container's managed
	/// reference. The returned product may be created during relational-model
	/// finalization and consumed later during SessionFactory construction.
	///
	/// The default implementation calls [#createGenerator(GeneratorCreationContext)]
	/// and wraps the resulting instance in an unmanaged direct reference. It is
	/// suitable only when `createGenerator()` does not obtain a container-managed
	/// reference whose identity or lifecycle must be retained.
	///
	/// @param context the mapping and bootstrap services for this usage site
	/// @return the prepared generator and its construction-scoped reference
	default PreparedGenerator<? extends Generator> prepareGenerator(GeneratorCreationContext context) {
		return PreparedGenerator.fromGenerator( createGenerator( context ) );
	}

	/// The effective runtime generator class, or `null` when a
	/// compatibility descriptor cannot resolve it without creating the
	/// generator.
	///
	/// This method must not instantiate the generator or access CDI. It may
	/// resolve a retained class name using the services in `context`.
	///
	/// @param context the current creation context; class-based descriptors
	/// may not need it
	/// @return the effective generator class, or `null` when it is unknown
	default Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
		return null;
	}

	/// Whether the effective generator contributes objects to the relational
	/// database model.
	///
	/// This is a classification operation and must not create the generator.
	/// An unknown effective class is conservatively considered exportable.
	default boolean isExportable(GeneratorCreationContext context) {
		final Class<? extends Generator> generatorClass = getGeneratorClass( context );
		return generatorClass == null || ExportableProducer.class.isAssignableFrom( generatorClass );
	}

	/// Apply generator semantics which affect the relational model without
	/// requiring access to a live generator instance.
	///
	/// This callback occurs before the coordinator asks
	/// [#requiresBootPreparation(GeneratorCreationContext)]. Direct built-in
	/// descriptors use it for facts such as identity-column handling. It must
	/// not instantiate, configure, or initialize a generator.
	default void applyRelationalModel(GeneratorCreationContext context) {
	}

	/// Whether relational-model finalization needs access to a generator
	/// instance. In addition to exportable generators, identifier generators
	/// which generate on execution are prepared early so their identity-column
	/// requirements can be applied to the mapping.
	///
	/// Returning `false` defers [#prepareGenerator] until
	/// SessionFactory construction. This method must not itself create or
	/// prepare a generator.
	default boolean requiresBootPreparation(GeneratorCreationContext context) {
		final Class<? extends Generator> generatorClass = getGeneratorClass( context );
		return generatorClass == null
				|| ExportableProducer.class.isAssignableFrom( generatorClass )
				|| OnExecutionGenerator.class.isAssignableFrom( generatorClass );
	}

	/// Does this descriptor create instances of [org.hibernate.generator.Assigned]?
	///
	/// This is a descriptor-level semantic query and must not create the
	/// generator.
	default boolean isAssigned() {
		return false;
	}
}
