/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;


/**
 * Copy of the model used in JoinTest, but using annotations rather than hbm.xml to look
 * for the duplicate joins warning
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {Person.class, Employee.class, Customer.class, User.class}
)
@SessionFactory
public class SecondaryTableTest {


	@Test
	public void testIt() {
		// really we have nothing to test
	}
}
