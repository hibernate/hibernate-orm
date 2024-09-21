/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.singletable;


/**
 * @author Gavin King
 */
public class Person extends Entity {
	public Address address;
	public Address mailingAddress;
}
