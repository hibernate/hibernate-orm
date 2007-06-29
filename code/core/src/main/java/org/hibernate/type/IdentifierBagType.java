//$Id: IdentifierBagType.java 10086 2006-07-05 18:17:27Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentIdentifierBag;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class IdentifierBagType extends CollectionType {

	public IdentifierBagType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
	}

	public PersistentCollection instantiate(
		SessionImplementor session,
		CollectionPersister persister, Serializable key)
		throws HibernateException {

		return new PersistentIdentifierBag(session);
	}

	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}
	
	public Class getReturnedClass() {
		return java.util.Collection.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentIdentifierBag( session, (java.util.Collection) collection );
	}

}






