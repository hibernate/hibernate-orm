package org.hibernate.bytecode.enhance.spi;

/// Controls generated enhancement features and handling of unsupported classes.
/// These choices are independent of the persistent attributes described by an
/// [EnhancementModel] and the bytecode supplied by an [EnhancementEnvironment].
///
/// An [EnhancementSession] may create enhancers with different options over the
/// same model. Options must remain stable for each enhancer's lifetime and support
/// concurrent queries when that enhancer is used concurrently. Client enhancers
/// use disabled managed-generation flags; client field-access rewriting is selected
/// by calling [Enhancer#enhanceClient(String, byte[])].
///
/// @see EnhancementSession#createEnhancer(EnhancementOptions)
/// @author Steve Ebersole
public interface EnhancementOptions {
	/// Creates immutable settings for the three managed-generation features.
	/// Extended enhancement remains disabled and the unsupported-class policy is
	/// [UnsupportedEnhancementStrategy#SKIP]. Passing false for all three flags
	/// supplies the managed-generation settings for a client enhancer.
	///
	/// @param dirtyTracking whether to generate inline dirty tracking
	/// @param lazyInitialization whether to generate lazy-loading support for
	///                           attributes permitted by the model
	/// @param associationManagement whether to generate bidirectional association management
	/// @return options exposing these flags through the no-argument feature methods
	static EnhancementOptions of(boolean dirtyTracking, boolean lazyInitialization, boolean associationManagement) {
		return new EnhancementOptions() {
			@Override
			public boolean doDirtyCheckingInline() {
				return dirtyTracking;
			}
			@Override
			public boolean doLazyInitialization() {
				return lazyInitialization;
			}
			@Override
			public boolean doBiDirectionalAssociationManagement() {
				return associationManagement;
			}
		};
	}

	/// Whether to generate lazy-loading support. Attribute eligibility is separately
	/// determined by [EnhancementModel#hasLazyLoadableAttributes(UnloadedClass)] and
	/// [EnhancementModel#isLazyLoadable(UnloadedField)].
	///
	/// @return true by default
	default boolean doLazyInitialization() {
		return true;
	}

	/// Selects the policy for unsupported mappings, such as property-access
	/// attributes whose accessor names do not match their underlying fields.
	/// This policy does not convert arbitrary transformation errors into skips.
	///
	/// @return [UnsupportedEnhancementStrategy#SKIP] by default
	default UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
		return UnsupportedEnhancementStrategy.SKIP;
	}

	/// Whether to generate inline tracking of changes to persistent attributes.
	/// The default delegates to the deprecated class-argument method with null.
	///
	/// @return true to generate dirty-tracking support
	default boolean doDirtyCheckingInline() {
		return doDirtyCheckingInline( null );
	}

	/// Whether the managed pass should also rewrite client field accesses using
	/// the legacy combined enhancement path. New integrations use the separate
	/// [Enhancer#enhanceClient(String, byte[])] operation.
	///
	/// @return false by default
	///
	/// @deprecated Use the separate [Enhancer#enhanceClient] operation instead.
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doExtendedEnhancement() {
		return doExtendedEnhancement( null );
	}

	/// Whether to generate automatic maintenance of the opposite side of a
	/// bidirectional association. The default delegates to the deprecated
	/// field-argument method with null.
	///
	/// @return false by default
	///
	/// @deprecated Will be removed without replacement. See HHH-19660
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doBiDirectionalAssociationManagement() {
		return doBiDirectionalAssociationManagement( null );
	}

	/// Legacy class-argument form of [#doDirtyCheckingInline()]. The no-argument
	/// default calls this method with null; enhancers do not use it to select
	/// different generation settings for individual classes.
	///
	/// @param classDescriptor a class descriptor, possibly null; ignored by the default implementation
	///
	/// @return true by default
	///
	/// @deprecated Use [#doDirtyCheckingInline()] instead.
	@Deprecated(forRemoval = true)
	default boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return true;
	}

	/// Legacy class-argument form of [#doExtendedEnhancement()]. The no-argument
	/// default calls this method with null.
	///
	/// @param classDescriptor a class descriptor, possibly null; ignored by the default implementation
	///
	/// @return false by default
	///
	/// @deprecated Use [#doExtendedEnhancement()] instead.
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		return false;
	}

	/// Legacy field-argument form of [#doBiDirectionalAssociationManagement()].
	/// The no-argument default calls this method with null; enhancers do not use it
	/// to select different generation settings for individual associations.
	///
	/// @param field a field descriptor, possibly null; ignored by the default implementation
	///
	/// @return false by default
	///
	/// @deprecated Use [#doBiDirectionalAssociationManagement()] instead.
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return false;
	}
}
