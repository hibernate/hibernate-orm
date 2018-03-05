/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.spi;

/**
 * @author Steve Ebersole
 */
public interface ContainedBean<B> {
	// todo (5.3) : can we combine ContainedBean and org.hibernate.resource.beans.spi.ManagedBean into the same thing?
	/**
	 * Get the bean instance producer associated with this container-backed bean
	 */
	B getBeanInstance();
}
