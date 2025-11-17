/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

/**
 * JPA entity listener
 *
 * @author Steve Ebersole
 */
public class GlobalEntityListener {
	public void entityCreated(Object entity) {
		System.out.println( "Entity was created - " + entity );
	}

	public Object entityCreated() {
		throw new RuntimeException( "Should not be called" );
	}

	public void entityCreated(Object e1, Object e2) {
		throw new RuntimeException( "Should not be called" );
	}
}
