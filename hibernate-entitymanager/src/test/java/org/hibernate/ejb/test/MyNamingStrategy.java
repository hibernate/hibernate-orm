//$Id$
package org.hibernate.ejb.test;
import org.hibernate.cfg.EJB3NamingStrategy;

/**
 * @author Emmanuel Bernard
 */
public class MyNamingStrategy extends EJB3NamingStrategy {
	@Override
	public String tableName(String tableName) {
		return "tbl_" + super.tableName( tableName );
	}
}
