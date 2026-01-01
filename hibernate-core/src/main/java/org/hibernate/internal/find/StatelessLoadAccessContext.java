/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import org.hibernate.engine.spi.StatelessSessionImplementor;

/**
 * @author Steve Ebersole
 */
public interface StatelessLoadAccessContext {
	StatelessSessionImplementor getStatelessSession();
}
