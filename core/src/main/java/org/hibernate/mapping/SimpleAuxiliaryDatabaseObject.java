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
package org.hibernate.mapping;

import java.util.HashSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.HibernateException;
import org.hibernate.util.StringHelper;

/**
 * A simple implementation of AbstractAuxiliaryDatabaseObject in which the CREATE and DROP strings are
 * provided up front.  Contains simple facilities for templating the catalog and schema
 * names into the provided strings.
 * <p/>
 * This is the form created when the mapping documents use &lt;create/&gt; and
 * &lt;drop/&gt;.
 *
 * @author Steve Ebersole
 */
public class SimpleAuxiliaryDatabaseObject extends AbstractAuxiliaryDatabaseObject {

	private final String sqlCreateString;
	private final String sqlDropString;

	public SimpleAuxiliaryDatabaseObject(String sqlCreateString, String sqlDropString) {
		this.sqlCreateString = sqlCreateString;
		this.sqlDropString = sqlDropString;
	}

	public SimpleAuxiliaryDatabaseObject(String sqlCreateString, String sqlDropString, HashSet dialectScopes) {
		super( dialectScopes );
		this.sqlCreateString = sqlCreateString;
		this.sqlDropString = sqlDropString;
	}

	public String sqlCreateString(
	        Dialect dialect,
	        Mapping p,
	        String defaultCatalog,
	        String defaultSchema) throws HibernateException {
		return injectCatalogAndSchema( sqlCreateString, defaultCatalog, defaultSchema );
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return injectCatalogAndSchema( sqlDropString, defaultCatalog, defaultSchema );
	}

	private String injectCatalogAndSchema(String ddlString, String defaultCatalog, String defaultSchema) {
		String rtn = StringHelper.replace( ddlString, "${catalog}", defaultCatalog );
		rtn = StringHelper.replace( rtn, "${schema}", defaultSchema );
		return rtn;
	}
}
