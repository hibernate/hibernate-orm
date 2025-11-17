/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.genericsinheritance;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		ChildHierarchy1.class,
		ParentHierarchy1.class,
		ChildHierarchy22.class,
		ParentHierarchy22.class
})
@SessionFactory
public class GenericsInheritanceTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inSession( sessionImplementor -> {
			//mapping is tested
		} );
	}

}
