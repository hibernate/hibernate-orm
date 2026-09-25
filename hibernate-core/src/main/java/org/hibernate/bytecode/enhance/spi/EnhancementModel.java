package org.hibernate.bytecode.enhance.spi;

import java.util.Set;

/// Describes the persistence semantics of classes and fields without defining
/// application classes. An [EnhancementSession] combines this model with an
/// [EnhancementEnvironment] and resolves discovered types, including implicit
/// embeddables. The model itself does not own discovery results or bytecode caches.
///
/// Model decisions describe persistent state independently of whether an enhancer
/// generates support for a particular feature. For example, a lazy attribute remains
/// lazy in the model when [EnhancementOptions#doLazyInitialization()] is false.
///
/// Implementations must provide stable decisions for a session's lifetime and
/// support concurrent queries when the session is used concurrently. A model may
/// be reused with different environments; resolved metadata is session-specific.
/// [DefaultEnhancementModel] supplies Hibernate's annotation-based defaults.
///
/// @since 8.0
/// @author Steve Ebersole
public interface EnhancementModel {

	/// Initial classes whose managed enhancement is planned by the integration.
	/// Sessions snapshot these names and may discover additional embedded types and
	/// mapped superclasses. Merely referencing another entity does not schedule it.
	///
	/// @return binary class names, using dots rather than JVM internal-name slashes;
	///         empty by default, allowing explicit [EnhancementSession#discoverTypes]
	///         calls to supply candidates
	default Set<String> getCandidates() {
		return Set.of();
	}

	/// @param type an unloaded class descriptor
	/// @return whether the class is an entity within this model
	boolean isEntityClass(UnloadedClass type);

	/// Identifies explicitly known embeddables. Session discovery additionally
	/// recognizes types used as embedded attributes, even without an embeddable annotation.
	///
	/// @param type an unloaded class descriptor
	/// @return whether the model explicitly recognizes an embeddable class
	boolean isCompositeClass(UnloadedClass type);

	/// @param type an unloaded class descriptor
	/// @return whether the class supplies persistent state as a mapped superclass
	boolean isMappedSuperclassClass(UnloadedClass type);

	/// Determines persistent-field eligibility after the enhancer has excluded
	/// static fields and its generated implementation fields.
	///
	/// @param field an unloaded field descriptor
	/// @return whether the field represents persistent state
	boolean isPersistentField(UnloadedField field);

	/// Orders persistent fields consistently with the model's attribute ordering.
	/// Implementations may reorder the supplied array in place. A replacement array
	/// must preserve its runtime component type and contain the supplied descriptors.
	///
	/// @param fields the eligible persistent fields
	/// @return those fields in model order
	UnloadedField[] order(UnloadedField[] fields);

	/// @param type an unloaded class descriptor
	/// @return whether the model permits lazy attributes in this class, independently
	///         of whether lazy-loading support is enabled in the enhancement options
	boolean hasLazyLoadableAttributes(UnloadedClass type);

	/// @param field an unloaded persistent-field descriptor
	/// @return whether the attribute may be loaded lazily under this model
	boolean isLazyLoadable(UnloadedField field);

	/// Distinguishes persistent plural attributes from collection-valued basic
	/// attributes when the enhancer handles a collection-typed field.
	///
	/// @param field an unloaded persistent-field descriptor
	/// @return whether the field should receive persistent-collection handling
	boolean isMappedCollection(UnloadedField field);
}
