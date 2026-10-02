package org.hibernate.orm.test.annotations.interfaces;
import java.util.Collection;

/**
 * @author Emmanuel Bernard
 */
public interface User {
	Integer getId();

	Collection<Contact> getContacts();


}
