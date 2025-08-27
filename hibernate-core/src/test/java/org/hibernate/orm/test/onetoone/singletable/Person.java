/*
 * SPDX-License-Identifier: Apache-2.0
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
