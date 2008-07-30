/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;

/**
 * Generates <tt>string</tt> values using the SQL Server NEWID() function.
 *
 * @author Joseph Fifield
 */
public class GUIDGenerator implements IdentifierGenerator {

	private static final Logger log = LoggerFactory.getLogger(GUIDGenerator.class);

	public Serializable generate(SessionImplementor session, Object obj) 
	throws HibernateException {
		
		final String sql = session.getFactory().getDialect().getSelectGUIDString();
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			try {
				ResultSet rs = st.executeQuery();
				final String result;
				try {
					rs.next();
					result = rs.getString(1);
				}
				finally {
					rs.close();
				}
				log.debug("GUID identifier generated: " + result);
				return result;
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not retrieve GUID",
					sql
				);
		}
	}

}
