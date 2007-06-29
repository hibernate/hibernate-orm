// $Id: SimpleAuxiliaryDatabaseObject.java 7800 2005-08-10 12:13:00Z steveebersole $
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
