// $Id$
package org.hibernate.test.annotations.namingstrategy;
import org.hibernate.cfg.EJB3NamingStrategy;

@SuppressWarnings("serial")
public class DummyNamingStrategy extends EJB3NamingStrategy {
	
	public String tableName(String tableName) {
		return "T" + tableName;
	}

}
