/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.order.NullPrecedence;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the {@link NullPrecedence} query option on order-bys.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-14981")
@Jpa(annotatedClasses = {StrIntTestEntity.class})
@EnversTest
public class NullPrecedenceTest {

	Integer id1;
	Integer id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		id1 = scope.fromTransaction( em -> {
			StrIntTestEntity entity1 = new StrIntTestEntity( null, 1 );
			em.persist( entity1 );
			return entity1.getId();
		} );
		// Revision 2
		id2 = scope.fromTransaction( em -> {
			StrIntTestEntity entity2 = new StrIntTestEntity( "two", 2 );
			em.persist( entity2 );
			return entity2.getId();
		} );
	}

	@Test
	public void testNullPrecedenceFirst(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery().forRevisionsOfEntity( StrIntTestEntity.class, true, false )
					.addProjection( AuditEntity.property( "number" ) )
					.addOrder( AuditEntity.property( "str1" ).asc().nulls( NullPrecedence.FIRST ) )
					.getResultList();
			List<Integer> expected = new ArrayList<>();
			expected.addAll( Arrays.asList( 1, 2 ) );
			assertEquals( expected, results );
		} );
	}

	@Test
	public void testNullPrecedenceLast(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery().forRevisionsOfEntity( StrIntTestEntity.class, true, false )
					.addProjection( AuditEntity.property( "number" ) )
					.addOrder( AuditEntity.property( "str1" ).asc().nulls( NullPrecedence.LAST ) )
					.getResultList();
			List<Integer> expected = new ArrayList<>();
			expected.addAll( Arrays.asList( 2, 1 ) );
			assertEquals( expected, results );
		} );
	}
}
