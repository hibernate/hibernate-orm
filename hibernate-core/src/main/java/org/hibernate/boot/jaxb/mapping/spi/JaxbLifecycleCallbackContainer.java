/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi;

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
	JaxbPrePersistImpl getPrePersist();
	void setPrePersist(JaxbPrePersistImpl value);

	JaxbPostPersistImpl getPostPersist();
	void setPostPersist(JaxbPostPersistImpl value);

	JaxbPreRemoveImpl getPreRemove();
	void setPreRemove(JaxbPreRemoveImpl value);

	JaxbPostRemoveImpl getPostRemove();
	void setPostRemove(JaxbPostRemoveImpl value);

	JaxbPreUpdateImpl getPreUpdate();
	void setPreUpdate(JaxbPreUpdateImpl value);

	JaxbPostUpdateImpl getPostUpdate();
	void setPostUpdate(JaxbPostUpdateImpl value);

	JaxbPostLoadImpl getPostLoad();
	void setPostLoad(JaxbPostLoadImpl value);
}
