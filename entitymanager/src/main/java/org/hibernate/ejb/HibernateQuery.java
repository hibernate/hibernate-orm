//$Id$
package org.hibernate.ejb;

import javax.persistence.Query;

public interface HibernateQuery extends Query {
	public org.hibernate.Query getHibernateQuery();
}
