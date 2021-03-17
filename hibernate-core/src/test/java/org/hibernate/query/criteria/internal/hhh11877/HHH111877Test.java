package org.hibernate.query.criteria.internal.hhh11877;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-11877" )
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
