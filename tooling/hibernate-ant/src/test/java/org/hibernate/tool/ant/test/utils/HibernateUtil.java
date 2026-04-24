/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.test.utils;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;

public class HibernateUtil {

	public static class Dialect extends org.hibernate.dialect.Dialect {
		public Dialect() {
			super( (DatabaseVersion) null );
		}
	}

	public static class ConnectionProvider extends UserSuppliedConnectionProviderImpl {
		private static final long serialVersionUID = 1L;
	}

}
