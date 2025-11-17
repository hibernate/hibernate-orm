/*
 * SPDX-License-Identifier: Apache-2.0
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
