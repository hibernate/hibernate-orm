/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.PostCreate;
import jakarta.persistence.PreClose;

/// Enumerates JPA persistence unit lifecycle callback types.
///
/// @author Gavin King
///
/// @since 8.0
public enum PersistenceUnitCallbackType {
	/// @see PostCreate
	POST_CREATE,

	/// @see PreClose
	PRE_CLOSE;

	/// The callback annotation type corresponding to this lifecycle callback type.
	public Class<? extends Annotation> getCallbackAnnotation() {
		return switch ( this ) {
			case POST_CREATE -> PostCreate.class;
			case PRE_CLOSE -> PreClose.class;
		};
	}
}
