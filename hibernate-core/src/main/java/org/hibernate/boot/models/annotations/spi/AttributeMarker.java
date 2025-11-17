/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.FetchType;

/**
 * Commonality for annotations which identify attributes.
 *
 * @apiNote All the interesting bits are in the optional sub-interfaces.
 *
 * @author Steve Ebersole
 */
public interface AttributeMarker extends Annotation {
	interface Fetchable extends AttributeMarker {
		FetchType fetch();

		void fetch(FetchType value);
	}

	interface Cascadeable extends AttributeMarker {
		jakarta.persistence.CascadeType[] cascade();

		void cascade(jakarta.persistence.CascadeType[] value);
	}

	interface Optionalable extends AttributeMarker {
		boolean optional();

		void optional(boolean value);
	}

	interface Mappable extends AttributeMarker {
		String mappedBy();

		void mappedBy(String value);
	}
}
