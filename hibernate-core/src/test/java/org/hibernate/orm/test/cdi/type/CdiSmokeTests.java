/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.type;

import org.hibernate.resource.beans.container.internal.NotYetReadyException;

import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class CdiSmokeTests {
	@Test
	void testCdiOperations() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( UrlType.class, OtherBean.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			final BeanManager beanManager = cdiContainer.getBeanManager();

			final AnnotatedType<UrlType> annotatedType;
			try {
				annotatedType = beanManager.createAnnotatedType( UrlType.class );
			}
			catch (Exception e) {
				throw new IllegalStateException( new NotYetReadyException( e ) );
			}

			final InjectionTarget<UrlType> injectionTarget = beanManager
					.getInjectionTargetFactory( annotatedType )
					.createInjectionTarget( null );
			final CreationalContext<UrlType> creationalContext = beanManager.createCreationalContext( null );

			final UrlType beanInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( beanInstance, creationalContext );

			injectionTarget.postConstruct( beanInstance );

			assertThat( beanInstance ).isNotNull();
//			assertThat( beanInstance.getOtherBean() ).isNotNull();
		}
	}
}
