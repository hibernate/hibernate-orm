/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassProxyInterface;


/**
 * @author Steve Ebersole
 */
public class Doctor extends Person implements IDoctor {
	public Doctor() {
	}

	public Doctor(String name) {
		super( name );
	}

	public String operate() {
		return "Dr. " + getName() + " is in";
	}
}
