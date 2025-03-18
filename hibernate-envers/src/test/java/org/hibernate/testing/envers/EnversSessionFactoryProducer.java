/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import org.hibernate.SessionFactory;

/**
 * Envers contract for something that can build a SessionFactory based on an audit strategy.
 *
 * @author Chris Cranford
 */
public interface EnversSessionFactoryProducer {
	SessionFactory produceSessionFactory(String auditStrategyName);
}
