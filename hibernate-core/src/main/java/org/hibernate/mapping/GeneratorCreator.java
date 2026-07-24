/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.generator.Assigned;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.models.spi.ModelsContext;

/**
 * Instantiates a {@link Generator}.
 *
 * @since 6.2
 *
 * @author Gavin King
 */
@Internal
@FunctionalInterface
public interface GeneratorCreator extends Serializable {
	/**
	 * Create the generator.
	 */
	Generator createGenerator(GeneratorCreationContext context);

	/**
	 * Reattach the Models context used to interpret retained boot-model annotations
	 * after this creator has been deserialized.
	 */
	default void reattachModelsContext(ModelsContext modelsContext) {
	}

	/**
	 * Does this object create instances of {@link Assigned}?
	 *
	 * @since 7.0
	 */
	default boolean isAssigned() {
		return false;
	}
}
