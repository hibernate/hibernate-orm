/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

/**
 * Descriptor for persistent collections.  This includes mapping
 * information, so it is specific to each usage (attribute).  JPA
 * has no construct as a type for collections
 *
 * @author Steve Ebersole
 */
public interface CollectionDomainType<C,E> extends DomainType<C> {
	interface Element<E> {
		/**
		 * The Java type of the collection elements.
		 */
		Class<E> getJavaType();
	}

	Element<E> getElementDescriptor();
}
