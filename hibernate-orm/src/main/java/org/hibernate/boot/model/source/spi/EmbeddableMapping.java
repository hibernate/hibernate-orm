/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;

/**
 * Unifying contract for consuming JAXB types which describe an embeddable (in JPA terms).
 *
 * @author Steve Ebersole
 */
public interface EmbeddableMapping {
	public String getClazz();
	public List<JaxbHbmTuplizerType> getTuplizer();

	public String getParent();
}
