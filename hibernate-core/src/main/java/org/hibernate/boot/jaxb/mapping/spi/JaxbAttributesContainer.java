/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
