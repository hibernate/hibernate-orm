/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping;

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
public interface LifecycleCallbackContainer {
	JaxbPrePersist getPrePersist();

	void setPrePersist(JaxbPrePersist value);

	JaxbPostPersist getPostPersist();

	void setPostPersist(JaxbPostPersist value);

	JaxbPreRemove getPreRemove();

	void setPreRemove(JaxbPreRemove value);

	JaxbPostRemove getPostRemove();

	void setPostRemove(JaxbPostRemove value);

	JaxbPreUpdate getPreUpdate();

	void setPreUpdate(JaxbPreUpdate value);

	JaxbPostUpdate getPostUpdate();

	void setPostUpdate(JaxbPostUpdate value);

	JaxbPostLoad getPostLoad();

	void setPostLoad(JaxbPostLoad value);
}
