/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * Contract for a container of {@link AttributeSource} references.  Entities,
 * MappedSuperclasses and composites (Embeddables) all contain attributes.
 * <p>
 * Think of this as the corollary to what JPA calls a ManagedType on the
 * source side of things.
 *
 * @author Steve Ebersole
 */
public interface AttributeSourceContainer extends ToolingHintContextContainer {
	AttributePath getAttributePathBase();
	AttributeRole getAttributeRoleBase();

	/**
	 * Obtain this container's attribute sources.
	 *
	 * @return The attribute sources.
	 */
	List<AttributeSource> attributeSources();

	/**
	 * Obtain the local binding context associated with this container.
	 *
	 * @return The local binding context
	 */
	LocalMetadataBuildingContext getLocalMetadataBuildingContext();
}
