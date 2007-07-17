//$Id: RelationalModel.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.mapping;

import org.hibernate.engine.Mapping;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * A relational object which may be created using DDL
 * @author Gavin King
 */
public interface RelationalModel {
	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) throws HibernateException;
	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema);
}
