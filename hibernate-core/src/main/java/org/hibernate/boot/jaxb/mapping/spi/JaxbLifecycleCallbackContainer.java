/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * JAXB binding interface for commonality between things which
 * allow callback declarations.  This includes <ul>
 *     <li>
 *         entities and mapped-superclasses
 *     </li>
 *     <li>
 *         entity-listener classes
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface JaxbLifecycleCallbackContainer {
	@Nullable JaxbPrePersistImpl getPrePersist();
	void setPrePersist(@Nullable JaxbPrePersistImpl value);

	@Nullable JaxbPostPersistImpl getPostPersist();
	void setPostPersist(@Nullable JaxbPostPersistImpl value);

	@Nullable JaxbPreRemoveImpl getPreRemove();
	void setPreRemove(@Nullable JaxbPreRemoveImpl value);

	@Nullable JaxbPostRemoveImpl getPostRemove();
	void setPostRemove(@Nullable JaxbPostRemoveImpl value);

	@Nullable JaxbPreUpdateImpl getPreUpdate();
	void setPreUpdate(@Nullable JaxbPreUpdateImpl value);

	@Nullable JaxbPostUpdateImpl getPostUpdate();
	void setPostUpdate(@Nullable JaxbPostUpdateImpl value);

	@Nullable JaxbPostLoadImpl getPostLoad();
	void setPostLoad(@Nullable JaxbPostLoadImpl value);
}
