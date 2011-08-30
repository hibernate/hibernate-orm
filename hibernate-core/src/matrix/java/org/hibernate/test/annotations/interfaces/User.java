//$Id$
package org.hibernate.test.annotations.interfaces;
import java.util.Collection;

/**
 * @author Emmanuel Bernard
 */
public interface User {
	Integer getId();

	Collection<Contact> getContacts();


}
