//$Id$
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;

/**
 * @author Gavin King
 */
public interface PostInsertIdentifierGenerator extends IdentifierGenerator {
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
	        Dialect dialect,
	        boolean isGetGeneratedKeysEnabled) throws HibernateException;
}
