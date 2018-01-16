/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.spi.ManagedBean;

interface CdiLifecycleManagementStrategy {

	<T> ManagedBean<T> createBean(BeanManager beanManager, Class<T> beanClass);

	<T> ManagedBean<T> createBean(BeanManager beanManager, String beanName, Class<T> beanClass);

}
