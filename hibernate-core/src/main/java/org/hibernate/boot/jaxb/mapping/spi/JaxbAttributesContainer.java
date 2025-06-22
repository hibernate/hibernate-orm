/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * JAXB binding interface for commonality between things which contain attributes.
 *
 * @apiNote In the mapping XSD, this equates to the `attributes` and `embeddable-attributes`
 * nodes rather than the ManagedTypes themselves.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbAttributesContainer extends JaxbBaseAttributesContainer {
	List<JaxbOneToOneImpl> getOneToOneAttributes();

	List<JaxbElementCollectionImpl> getElementCollectionAttributes();

	List<JaxbOneToManyImpl> getOneToManyAttributes();

	List<JaxbManyToManyImpl> getManyToManyAttributes();

	List<JaxbPluralAnyMappingImpl> getPluralAnyMappingAttributes();

	List<JaxbTransientImpl> getTransients();
}
