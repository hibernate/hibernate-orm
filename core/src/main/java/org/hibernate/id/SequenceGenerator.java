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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

/**
 * <b>sequence</b><br>
 * <br>
 * Generates <tt>long</tt> values using an oracle-style sequence. A higher
 * performance algorithm is <tt>SequenceHiLoGenerator</tt>.<br>
 * <br>
 * Mapping parameters supported: sequence, parameters.
 *
 * @see SequenceHiLoGenerator
 * @see TableHiLoGenerator
 * @author Gavin King
 */

public class SequenceGenerator implements PersistentIdentifierGenerator, Configurable {

	/**
	 * The sequence parameter
	 */
	public static final String SEQUENCE = "sequence";

	/**
	 * The parameters parameter, appended to the create sequence DDL.
	 * For example (Oracle): <tt>INCREMENT BY 1 START WITH 1 MAXVALUE 100 NOCACHE</tt>.
	 */
	public static final String PARAMETERS = "parameters";

	private String sequenceName;
	private String parameters;
	private Type identifierType;
	private String sql;

	private static final Logger log = LoggerFactory.getLogger(SequenceGenerator.class);

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		sequenceName = PropertiesHelper.getString(SEQUENCE, params, "hibernate_sequence");
		parameters = params.getProperty(PARAMETERS);
		String schemaName = params.getProperty(SCHEMA);
		String catalogName = params.getProperty(CATALOG);

		if (sequenceName.indexOf( '.' ) < 0) {
			sequenceName = Table.qualify( catalogName, schemaName, sequenceName );
		}

		this.identifierType = type;
		sql = dialect.getSequenceNextValString(sequenceName);
	}

	public Serializable generate(SessionImplementor session, Object obj) 
	throws HibernateException {
		
		try {

			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			try {
				ResultSet rs = st.executeQuery();
				try {
					rs.next();
					Serializable result = IdentifierGeneratorFactory.get(
							rs, identifierType
						);
					if ( log.isDebugEnabled() ) {
						log.debug("Sequence identifier generated: " + result);
					}
					return result;
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
			
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not get next sequence value",
					sql
				);
		}

	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		String[] ddl = dialect.getCreateSequenceStrings(sequenceName);
		if ( parameters != null ) {
			ddl[ddl.length - 1] += ' ' + parameters;
		}
		return ddl;
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings(sequenceName);
	}

	public Object generatorKey() {
		return sequenceName;
	}

	public String getSequenceName() {
		return sequenceName;
	}

}
