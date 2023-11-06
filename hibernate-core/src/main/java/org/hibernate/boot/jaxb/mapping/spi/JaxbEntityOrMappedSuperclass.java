/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for commonality between entity and mapped-superclass mappings
 *
 * @author Steve Ebersole
 */
public interface JaxbEntityOrMappedSuperclass extends JaxbManagedType, JaxbLifecycleCallbackContainer {
	JaxbIdClassImpl getIdClass();

	void setIdClass(JaxbIdClassImpl value);

	JaxbEmptyTypeImpl getExcludeDefaultListeners();

	void setExcludeDefaultListeners(JaxbEmptyTypeImpl value);

	JaxbEmptyTypeImpl getExcludeSuperclassListeners();

	void setExcludeSuperclassListeners(JaxbEmptyTypeImpl value);

	JaxbEntityListenersImpl getEntityListeners();

	void setEntityListeners(JaxbEntityListenersImpl value);
}
