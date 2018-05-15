/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.access;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.access.AttributeAccessorEntity;
import org.hibernate.envers.test.support.domains.access.SimpleAttributeAccessorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12063")
public class AttributeAccessorTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AttributeAccessorEntity.class };
	}

	@DynamicBeforeAll
	public void testAttributeAccessorInvoked() {
		// verify that the accessor was triggered during metadata building phase.
		assertThat( getMetamodel(), notNullValue() );
		assertThat( SimpleAttributeAccessorImpl.invoked, is( true ) );
	}

	@DynamicTest
	public void testCreateAndQueryAuditEntityWithAttributeAccessor() {
		doInHibernate( this::sessionFactory, session -> {
			final AttributeAccessorEntity entity = new AttributeAccessorEntity( 1, "ABC" );
			session.save( entity );
		} );

		final AttributeAccessorEntity ver1 = getAuditReader().find( AttributeAccessorEntity.class, 1, 1 );
		assertThat( ver1.getName(), is( "ABC" ) );
	}
}
