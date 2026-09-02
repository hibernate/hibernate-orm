/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;

/// Standard JDBC REF_CURSOR registration and typed extraction.
///
/// @author Steve Ebersole
public final class StandardRefCursorSupport implements RefCursorSupport {
	private final RefCursorSupportCreationContext creationContext;

	public StandardRefCursorSupport(RefCursorSupportCreationContext creationContext) {
		this.creationContext = creationContext;
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		try {
			statement.registerOutParameter( position, Types.REF_CURSOR );
		}
		catch (SQLException e) {
			throw creationContext.convert( e, "Error registering REF_CURSOR parameter [" + position + "]" );
		}
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		try {
			statement.registerOutParameter( name, Types.REF_CURSOR );
		}
		catch (SQLException e) {
			throw creationContext.convert( e, "Error registering REF_CURSOR parameter [" + name + "]" );
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		try {
			return statement.getObject( position, ResultSet.class );
		}
		catch (Exception e) {
			throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + position + "]", e );
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		try {
			return statement.getObject( name, ResultSet.class );
		}
		catch (Exception e) {
			throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + name + "]", e );
		}
	}
}
