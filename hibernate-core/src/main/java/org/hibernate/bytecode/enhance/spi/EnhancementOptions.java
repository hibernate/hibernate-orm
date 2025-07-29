/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

/**
 * Options for the {@linkplain Enhancer enhancement} process.
 *
 * @see EnhancementContext
 *
 * @author Steve Ebersole
 */
public interface EnhancementOptions {
	/**
	 * Whether to enable support for inline dirtiness checking.
	 */
	default boolean doDirtyCheckingInline() {
		return doDirtyCheckingInline( null );
	}

	/**
	 * Whether to enable support for extended enhancement.
	 *
	 * @deprecated Will be removed without replacement. See HHH-19661
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doExtendedEnhancement() {
		return doExtendedEnhancement( null );
	}

	/**
	 * Whether to enable support for automatic management of bidirectional associations.
	 *
	 * @deprecated Will be removed without replacement. See HHH-19660
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	default boolean doBiDirectionalAssociationManagement() {
		return doBiDirectionalAssociationManagement( null );
	}

	/**
	 * Should we in-line dirty checking for persistent attributes for this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that dirty checking should be in-lined within the entity; {@code false}
	 *         indicates it should not.  In-lined is more easily serializable and probably more performant.
	 *
	 * @deprecated Use {@linkplain #doDirtyCheckingInline()} instead.
	 */
	@Deprecated(forRemoval = true)
	boolean doDirtyCheckingInline(UnloadedClass classDescriptor);

	/**
	 * Should we enhance field access to entities from this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that any direct access to fields of entities should be routed to the enhanced
	 *         getter / setter  method.
	 *
	 * @deprecated Use {@linkplain #doExtendedEnhancement()} instead.
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	boolean doExtendedEnhancement(UnloadedClass classDescriptor);

	/**
	 * Whether to enable support for automatic management of bidirectional associations for this field.
	 *
	 * @param field The field to check.
	 *
	 * @return {@code true} indicates that the field is enhanced so that for bidirectional persistent fields
	 * the association is managed, i.e. the associations are automatically set; {@code false} indicates that
	 * the management is handled by the user.
	 *
	 * @deprecated Use {@linkplain #doBiDirectionalAssociationManagement()} instead.
	 */
	@Deprecated(forRemoval = true, since = "7.1")
	boolean doBiDirectionalAssociationManagement(UnloadedField field);
}
