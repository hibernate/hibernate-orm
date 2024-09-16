/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.interfaces;
import java.util.Collection;

/**
 * @author Emmanuel Bernard
 */
public interface User {
	Integer getId();

	Collection<Contact> getContacts();


}
