/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.jboss.logging.Logger;

/**
 * abstract base class for the metadatadialects to hold the
 * basic setup classes.
 *  
 * @author max
 *
 */
public abstract class AbstractMetaDataDialect implements RevengDialect {

	protected final Logger log = Logger.getLogger(this.getClass());

	private Connection connection;
	private DatabaseMetaData metaData;

	private ConnectionProvider connectionProvider = null;

	public void configure(
			ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public void close() {
		metaData = null;
		if(connection != null) {
			try {
				connectionProvider.closeConnection(connection);
			}
			catch (SQLException e) {
				throw new RuntimeException("Problem while closing connection", e);
			} finally {
				connection = null;
			}
		}
		connectionProvider = null;
	}

	protected DatabaseMetaData getMetaData() {
		if (metaData == null) {
			try {
				metaData = getConnection().getMetaData();
			}
			catch (SQLException e) {
				throw new RuntimeException("Getting database metadata", e);
			}
		}
		return metaData;
	}

	protected String getDatabaseStructure(String catalog, String schema) {
		ResultSet schemaRs = null;
		ResultSet catalogRs = null;
		String nl = System.lineSeparator();
		StringBuilder sb = new StringBuilder(nl);
		// Let's give the user some feedback. The exception
		// is probably related to incorrect schema configuration.
		sb.append("Configured schema:").append(schema).append(nl);
		sb.append("Configured catalog:").append(catalog ).append(nl);

		try {
			schemaRs = getMetaData().getSchemas();
			sb.append("Available schemas:").append(nl);
			while (schemaRs.next() ) {
				sb.append("  ").append(schemaRs.getString("TABLE_SCHEM") ).append(nl);
			}
		}
		catch (SQLException e2) {
			log.warn("Could not get schemas", e2);
			sb.append("  <SQLException while getting schemas>").append(nl);
		}
		finally {
			try {
				if (schemaRs != null) {
					schemaRs.close();
				}
			}
			catch (Exception ignore) {
			}
		}

		try {
			catalogRs = getMetaData().getCatalogs();
			sb.append("Available catalogs:").append(nl);
			while (catalogRs.next() ) {
				sb.append("  ").append(catalogRs.getString("TABLE_CAT") ).append(nl);
			}
		}
		catch (SQLException e2) {
			log.warn("Could not get catalogs", e2);
			sb.append("  <SQLException while getting catalogs>").append(nl);
		}
		finally {
			try {
				if (catalogRs != null) {
					catalogRs.close();
				}
			}
			catch (Exception ignore) {
			}
		}
		return sb.toString();
	}

	protected Connection getConnection() throws SQLException {
		if(connection==null) {
			connection = connectionProvider.getConnection();
		}
		return connection;
	}

	public void close(Iterator<?> iterator) {
		if(iterator instanceof ResultSetIterator) {
			((ResultSetIterator)iterator).close();
		}
	}

	public boolean needQuote(String name) {

		if(name==null) return false;

		// TODO: use jdbc metadata to decide on this. but for now we just handle the most typical cases.
		if(name.indexOf('-')>0) return true;
		if(name.indexOf(' ')>0) return true;
		return name.indexOf( '.' ) > 0;
	}

	protected String caseForSearch(String value) throws SQLException  {
		// TODO: handle quoted requests (just strip it ?)
		if(needQuote(value)) {
			if ( getMetaData().storesMixedCaseQuotedIdentifiers() ) {
				return value;
			} else if ( getMetaData().storesUpperCaseQuotedIdentifiers() ) {
				return toUpperCase( value );
			} else if( getMetaData().storesLowerCaseQuotedIdentifiers() ) {
				return toLowerCase( value );
			} else {
				return value;
			}
		} else if ( getMetaData().storesMixedCaseQuotedIdentifiers() ) {
			return value;
		} else if ( getMetaData().storesUpperCaseIdentifiers() ) {
			return toUpperCase( value );
		} else if( getMetaData().storesLowerCaseIdentifiers() ) {
			return toLowerCase( value );
		} else {
			return value;
		}
	}

	private String toUpperCase(String str) {
		return str==null ? null : str.toUpperCase();
	}

	private String toLowerCase(String str) {
		return str == null ? null : str.toLowerCase(Locale.ENGLISH);
	}

	public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put( "TABLE_CAT", catalog );
		m.put( "TABLE_SCHEMA", schema );
		m.put( "TABLE_NAME", table );
		m.put( "HIBERNATE_STRATEGY", null );
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		l.add(m);
		return l.iterator();
	}
}
