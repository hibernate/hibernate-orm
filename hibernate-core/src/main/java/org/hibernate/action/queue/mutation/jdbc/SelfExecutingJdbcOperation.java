/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface SelfExecutingJdbcOperation extends JdbcOperation {
	void execute(SharedSessionContractImplementor session);
}
