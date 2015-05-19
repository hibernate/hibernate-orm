/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

/**
 * Contract for a container of {@link AttributeSource} references.  Entities,
 * MappedSuperclasses and composites (Embeddables) all contain attributes.
 * <p/>
 * Think of this as the corollary to what JPA calls a ManagedType on the
 * source side of things.
 *
 * @author Steve Ebersole
 */
public interface AttributeSourceContainer extends ToolingHintContextContainer {
	public AttributePath getAttributePathBase();
	public AttributeRole getAttributeRoleBase();

	/**
	 * Obtain this container's attribute sources.
	 *
	 * @return The attribute sources.
	 */
	public List<AttributeSource> attributeSources();

	/**
	 * Obtain the local binding context associated with this container.
	 *
	 * @return The local binding context
	 */
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext();
}
