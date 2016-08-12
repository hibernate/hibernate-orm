/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.sql.Connection;

import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * A DdlTransactionIsolator implementations for use in cases where the
 * isolated Connection is shared.
 *
 * @author Steve Ebersole
 */
public class DdlTransactionIsolatorSharedImpl implements DdlTransactionIsolator {
	private final DdlTransactionIsolator wrappedIsolator;

	public DdlTransactionIsolatorSharedImpl(DdlTransactionIsolator ddlTransactionIsolator) {
		this.wrappedIsolator = ddlTransactionIsolator;
	}

	@Override
	public JdbcContext getJdbcContext() {
		return wrappedIsolator.getJdbcContext();
	}

	@Override
	public void prepare() {
		// skip delegating the call to prepare
	}

	@Override
	public Connection getIsolatedConnection() {
		return wrappedIsolator.getIsolatedConnection();
	}

	@Override
	public void release() {
		// skip delegating the call to prepare
	}
}
