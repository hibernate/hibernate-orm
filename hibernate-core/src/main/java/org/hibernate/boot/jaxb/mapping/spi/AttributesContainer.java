/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * Common interface for JAXB bindings which are containers of attributes.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface AttributesContainer {

	List<JaxbTransient> getTransient();

	List<JaxbBasic> getBasic();

	List<JaxbElementCollection> getElementCollection();

	List<JaxbEmbedded> getEmbedded();

	List<JaxbManyToMany> getManyToMany();

	List<JaxbManyToOne> getManyToOne();

	List<JaxbOneToMany> getOneToMany();

	List<JaxbOneToOne> getOneToOne();

}
