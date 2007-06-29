//$Id: PostInsertIdentifierGenerator.java 9681 2006-03-24 18:10:04Z steve.ebersole@jboss.com $
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
