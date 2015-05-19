/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc;

import org.hibernate.resource.jdbc.internal.LogicalConnectionProvidedImpl;

import org.hibernate.test.resource.common.DatabaseConnectionInfo;

/**
 * @author Steve Ebersole
 */
public class LogicalConnectionTestingImpl extends LogicalConnectionProvidedImpl {
	public LogicalConnectionTestingImpl() throws Exception {
		super( DatabaseConnectionInfo.INSTANCE.makeConnection() );
	}
}
