/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.jboss.as.jpa.hibernate5.service;

import java.util.Map;

import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Custom JtaPlatform initiator for use inside WildFly picking an appropriate
 * fallback JtaPlatform.
 *
 * @author Steve Ebersole
 */
public class CustomJtaPlatformInitiator extends JtaPlatformInitiator {
	@Override
	protected JtaPlatform getFallbackProvider(Map configurationValues, ServiceRegistryImplementor registry) {
		return new org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform();
	}
}
