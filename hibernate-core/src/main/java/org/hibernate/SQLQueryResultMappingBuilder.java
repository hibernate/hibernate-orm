/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.type.Type;

/**
 * Allows programmatic access from {@link SQLQuery} to define how to map SQL {@link java.sql.ResultSet results}
 * back to in-memory objects, both entities as well as scalars.  Essentially it defines an API akin to the
 * {@code <return/>}, {@code <return-scalar/>} and {@code <return-join/>} elements under {@code <sql-query/>}
 * definition in a Hibernate <tt>hbm.xml</tt> file.
 *
 * @author Steve Ebersole
 */
public interface SQLQueryResultMappingBuilder {

	public static interface ReturnsHolder {
		public void add(NativeSQLQueryReturn queryReturn);
	}

	public static class ScalarReturn {
		private final ReturnsHolder returnsHolder;
		private String name;
		private Type type;

		public ScalarReturn(ReturnsHolder returnsHolder) {
			this.returnsHolder = returnsHolder;
		}
	}

}
