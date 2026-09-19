/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.ordering.TranslationContext;

/**
 * Represents an individual identifier in a dot-identifier sequence
 *
 * @author Steve Ebersole
 */
public interface SequencePart {
	@Nullable
	SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext);
}
