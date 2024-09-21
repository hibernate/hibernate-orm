/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withinheritance;

/**
 * @author Hardy Ferentschik
 */
public class Ref extends AbstractRef {
	public Ref() {
	}

	public Ref(int id) {
		super( id );
	}
}
