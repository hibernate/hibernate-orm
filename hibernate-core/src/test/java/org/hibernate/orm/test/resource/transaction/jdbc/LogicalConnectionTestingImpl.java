/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;

import org.hibernate.orm.test.resource.common.DatabaseConnectionInfo;
import org.hibernate.resource.jdbc.internal.LogicalConnectionProvidedImpl;
import org.hibernate.resource.jdbc.internal.ResourceRegistryStandardImpl;

/**
 * @author Steve Ebersole
 */
public class LogicalConnectionTestingImpl extends LogicalConnectionProvidedImpl {
	public LogicalConnectionTestingImpl() throws Exception {
		super( DatabaseConnectionInfo.INSTANCE.makeConnection(), new ResourceRegistryStandardImpl() );
	}
}
