/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.Internal;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * For a full explanation of the purpose of this interface see {@link ManagedTypeHelper}.
 *
 * @apiNote This is an internal, private marking interface; it's exposed in the SPI
 *          package as bytecode enhanced user code needs to be able to reference it.
 *
 * @author Sanne Grinovero
 */
@Internal
public interface PrimeAmongSecondarySupertypes {

	default @Nullable ManagedEntity asManagedEntity() {
		return null;
	}

	default @Nullable PersistentAttributeInterceptable asPersistentAttributeInterceptable() {
		return null;
	}

	default @Nullable SelfDirtinessTracker asSelfDirtinessTracker() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default @Nullable Managed asManaged() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default @Nullable ManagedComposite asManagedComposite() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default @Nullable ManagedMappedSuperclass asManagedMappedSuperclass() {
		return null;
	}

	default @Nullable CompositeOwner asCompositeOwner() {
		return null;
	}

	default @Nullable CompositeTracker asCompositeTracker() {
		return null;
	}

	default @Nullable HibernateProxy asHibernateProxy() {
		return null;
	}

	default @Nullable ProxyConfiguration asProxyConfiguration() {
		return null;
	}

}
