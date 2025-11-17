/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.interfaces;


import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				ContactImpl.class, UserImpl.class
		}
)
public class InterfacesTest {
	@Test
	public void testInterface() {
		// test via SessionFactory building
	}

}
