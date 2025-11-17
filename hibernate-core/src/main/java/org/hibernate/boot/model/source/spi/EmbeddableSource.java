/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * Represents the binding source for an "embeddable" (in JPA terms)
 * or "composite" (in legacy Hibernate terms).
 * <p>
 * Note that this really models the JPA concept of an Embedded, more
 * than the Embeddable.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableSource extends AttributeSourceContainer {
	JavaTypeDescriptor getTypeDescriptor();

	String getParentReferenceAttributeName();

	/**
	 * Indicates whether this embeddable/component is dynamic (represented as a Map),
	 * or whether a dedicated class for it is available.
	 *
	 * @return {@code true} indicates that the composition is represented as a Map;
	 * {@code false} indicates there is a dedicated class for representing the
	 * composition.
	 */
	boolean isDynamic();

	boolean isUnique();
}
