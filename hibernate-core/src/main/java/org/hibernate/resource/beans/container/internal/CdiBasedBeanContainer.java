/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.BeanContainer;

/**
 * @author Steve Ebersole
 */
public interface CdiBasedBeanContainer extends BeanContainer {
	BeanManager getUsableBeanManager();
}
