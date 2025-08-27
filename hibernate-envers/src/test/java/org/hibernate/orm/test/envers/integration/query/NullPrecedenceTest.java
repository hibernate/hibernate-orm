/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.order.NullPrecedence;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;

/**
 * Tests for the {@link NullPrecedence} query option on order-bys.
 *
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-14981" )
public class NullPrecedenceTest extends BaseEnversJPAFunctionalTestCase {

	Integer id1;
	Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrIntTestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		id1 = TransactionUtil.doInJPA(this::entityManagerFactory, entityManager -> {
			StrIntTestEntity entity1 = new StrIntTestEntity( null, 1 );
			entityManager.persist( entity1 );
			return entity1.getId();
		} );
		// Revision 2
		id2 = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			StrIntTestEntity entity2 = new StrIntTestEntity( "two", 2 );
			entityManager.persist( entity2 );
			return entity2.getId();
		} );
	}

	@Test
	public void testNullPrecedenceFirst() {
		List results = getAuditReader().createQuery().forRevisionsOfEntity( StrIntTestEntity.class, true, false )
				.addProjection( AuditEntity.property( "number" ) )
				.addOrder( AuditEntity.property( "str1" ).asc().nulls( NullPrecedence.FIRST ) )
				.getResultList();
		List<Integer> expected = new ArrayList<>();
		expected.addAll( Arrays.asList( 1, 2 ) );
		Assert.assertEquals( expected, results );
	}

	@Test
	public void testNullPrecedenceLast() {
		List results = getAuditReader().createQuery().forRevisionsOfEntity( StrIntTestEntity.class, true, false )
				.addProjection( AuditEntity.property( "number" ) )
				.addOrder( AuditEntity.property( "str1" ).asc().nulls( NullPrecedence.LAST ) )
				.getResultList();
		List<Integer> expected = new ArrayList<>();
		expected.addAll( Arrays.asList( 2, 1 ) );
		Assert.assertEquals( expected, results );
	}
}
