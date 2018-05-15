/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.Metamodel;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicNonAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class NoneAuditedTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicNonAuditedEntity.class };
	}

	@DynamicTest
	public void testRevisionInfoTableNotCreated() {
		final Metamodel metamodel = sessionFactoryScope().getSessionFactory().getMetamodel();
		assertThat( metamodel.getEntities().size(), is( 1 ) );
		assertThat( metamodel.findEntityDescriptor( BasicNonAuditedEntity.class ), notNullValue() );
	}
}
