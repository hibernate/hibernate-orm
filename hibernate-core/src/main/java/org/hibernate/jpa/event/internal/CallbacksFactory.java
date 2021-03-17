/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.event.spi.CallbackBuilder;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * The intent of this class is to use a lighter implementation
 * when JPA callbacks are disabled via
 * {@link org.hibernate.boot.spi.SessionFactoryOptions#areJPACallbacksEnabled()}
 */
public final class CallbacksFactory {
	public static CallbackRegistryImplementor buildCallbackRegistry(SessionFactoryOptions options) {
		if ( jpaCallBacksEnabled( options ) ) {
			return new CallbackRegistryImpl();
		}
		else {
			return new EmptyCallbackRegistryImpl();
		}
	}

	public static CallbackBuilder buildCallbackBuilder(
			SessionFactoryOptions options,
			ServiceRegistry serviceRegistry,
			ReflectionManager reflectionManager) {
		if ( jpaCallBacksEnabled( options ) ) {
			final ManagedBeanRegistry managedBeanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
			return new CallbackBuilderLegacyImpl(
					managedBeanRegistry,
					reflectionManager
			);
		}
		else {
			return new EmptyCallbackBuilder();
		}
	}

	private static boolean jpaCallBacksEnabled(SessionFactoryOptions options) {
		return options.areJPACallbacksEnabled();
	}

}
