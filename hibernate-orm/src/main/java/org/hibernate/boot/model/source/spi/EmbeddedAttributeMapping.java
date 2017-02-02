/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.jaxb.hbm.spi.SingularAttributeInfo;

/**
 * Unifying contract for any JAXB types which describe an embedded (in JPA terms).
 * <p/>
 * Essentially this presents a unified contract over the {@code <component/>},
 * {@code <composite-id/>}, {@code <dynamic-component/>} and
 * {@code <nested-dynamic-component/>} elements
 *
 * @author Steve Ebersole
 */
public interface EmbeddedAttributeMapping extends SingularAttributeInfo {
	boolean isUnique();
	EmbeddableMapping getEmbeddableMapping();
}
