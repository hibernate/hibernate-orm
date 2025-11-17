/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface ManagedBeanSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Used to pass a CDI {@link jakarta.enterprise.inject.spi.BeanManager} to
	 * Hibernate.
	 * <p>
	 * According to the JPA specification, the {@code BeanManager} should be
	 * passed at boot time and be ready for immediate use at that time. But
	 * not all environments can do this (WildFly, for example). To accommodate
	 * such environments, Hibernate provides two options: <ol>
	 *     <li> A proprietary CDI extension SPI (which has been proposed to the CDI
	 *          spec group as a standard option) which can be used to provide delayed
	 *          {@code BeanManager} access: to use this solution, the reference passed
	 *          as the {@code BeanManager} during bootstrap should be typed as
	 *          {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 *     <li> Delayed access to the {@code BeanManager} reference: here, Hibernate
	 *          will not access the reference passed as the {@code BeanManager} during
	 *          bootstrap until it is first needed. Note, however, that this has the
	 *          effect of delaying the detection of any deployment problems until after
	 *          bootstrapping.
	 * </ol>
	 *
	 * This setting is used to configure access to the {@code BeanManager},
	 * either directly, or via
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyBeanManager(Object)
	 */
	String JAKARTA_CDI_BEAN_MANAGER = "jakarta.persistence.bean.manager";
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Identifies a {@link org.hibernate.resource.beans.container.spi.BeanContainer}
	 * to be used.
	 * <p>
	 * Note that for CDI-based containers setting this is not necessary - simply
	 * pass the {@link jakarta.enterprise.inject.spi.BeanManager} to use via
	 * {@link #JAKARTA_CDI_BEAN_MANAGER} and optionally specify {@link #DELAY_CDI_ACCESS}.
	 * This setting is useful to integrate non-CDI bean containers such as Spring.
	 *
	 * @since 5.3
	 */
	String BEAN_CONTAINER = "hibernate.resource.beans.container";

	/**
	 * Used in conjunction with {@value #BEAN_CONTAINER} when CDI is used.
	 * <p>
	 * By default, to be JPA spec compliant, Hibernate should access the CDI
	 * {@link jakarta.enterprise.inject.spi.BeanManager} while bootstrapping the
	 * {@link org.hibernate.SessionFactory}.  In some cases however this can lead
	 * to a chicken/egg situation where the JPA provider immediately accesses the
	 * {@code BeanManager} when managed beans are awaiting JPA PU injection.
	 * <p>
	 * This setting tells Hibernate to delay accessing until first use.
	 * <p>
	 * This setting has the decided downside that bean config problems will not
	 * be done at deployment time, but will instead manifest at runtime. For this
	 * reason, the preferred means for supplying a CDI BeanManager is to provide
	 * an implementation of
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager} which
	 * gives Hibernate a callback when the {@code BeanManager} is ready for use.
	 *
	 * @since 5.0.8
	 */
	String DELAY_CDI_ACCESS = "hibernate.delay_cdi_access";

	/**
	 * Controls whether Hibernate can try to create beans other than converters and
	 * listeners using CDI. Only meaningful when a CDI {@link #BEAN_CONTAINER container}
	 * is used.
	 * <p>
	 * By default, Hibernate will only attempt to create converter and listener beans using CDI.
	 *
	 * @since 6.2
	 */
	String ALLOW_EXTENSIONS_IN_CDI = "hibernate.cdi.extensions";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA setting
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #JAKARTA_CDI_BEAN_MANAGER} instead
	 */
	@Deprecated
	String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";
}
