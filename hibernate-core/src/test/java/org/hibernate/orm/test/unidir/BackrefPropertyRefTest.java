/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-9370")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/unidir/ParentChildPropertyRef.hbm.xml"
)
@SessionFactory
public class BackrefPropertyRefTest extends BackrefTest {
	@Test
	void verifyMapping(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		final PersistentClass entityBinding = modelScope.getEntityBinding( Parent.class );
		assertEquals( "parent_with_prop_ref", entityBinding.getRootTable().getName() );
	}

	@Override
	@FailureExpected(jiraKey = "HHH-9370")
	public void testBackRef(SessionFactoryScope factoryScope) {
		super.testBackRef( factoryScope );
	}

	@Override
	@FailureExpected(jiraKey = "HHH-9370")
	public void testBackRefToProxiedEntityOnMerge(SessionFactoryScope factoryScope) {
		super.testBackRefToProxiedEntityOnMerge( factoryScope );
	}
}
