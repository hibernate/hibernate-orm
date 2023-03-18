/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package for testing requests of CDI beans in Hibernate Search.
 *
 * In Hibernate Search,
 * beans are retrieved directly from the {@link org.hibernate.resource.beans.container.spi.BeanContainer}
 * because Hibernate Search is not bound by the JPA spec
 * and wants to leave the lifecycle of beans up to CDI instead
 * of controlling it in {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry}.
 * This involves using {@code canUseCachedReferences = false} and {@code useJpaCompliantCreation = false}
 * in {@link org.hibernate.resource.beans.container.spi.BeanContainer.LifecycleOptions}).
 *
 * Mainly these are regression tests against Hibernate Search's pattern of usage of ORM's
 * {@link org.hibernate.resource.beans.container.spi.BeanContainer} as accessed
 * via {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry#getBeanContainer()}.
 *
 * @see org.hibernate.orm.test.cdi.general.hibernatesearch.HibernateSearchSimulatedIntegrator
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch;
