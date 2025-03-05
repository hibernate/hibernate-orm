/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh11877;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-11877" )
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class HHH111877Test extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Test
	public void testNoExceptionThrow() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );

			cq.select( foo ).where( cb.and( cb.and(), foo.get( Foo_.bar ) ) );

			final TypedQuery<Foo> tq = entityManager.createQuery( cq );

			// without fixing, the statement below will throw exception:
			// java.lang.IllegalArgumentException: org.hibernate.hql.internal.ast.QuerySyntaxException: unexpected AST node: . near line 1, column 106 [select generatedAlias0 from org.hibernate.bugs.Foo as generatedAlias0 where ( 1=1 ) and ( generatedAlias0.bar )]
			tq.getResultList();
		} );
	}
}
