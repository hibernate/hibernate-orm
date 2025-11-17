/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.List;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-13111")
@Jpa(
		annotatedClasses = {AbstractSubqueryInSelectClauseTest.Person.class, AbstractSubqueryInSelectClauseTest.Document.class}
)
public class SubqueryInSelectClauseTest extends AbstractSubqueryInSelectClauseTest {

	@Test
	public void testSubqueryInSelectClause(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Document> document = query.from( Document.class );
			Join<?, ?> contacts = document.join( "contacts", JoinType.LEFT );

			Subquery<Long> personCount = query.subquery( Long.class );
			Root<Person> person = personCount.from( Person.class );
			personCount.select( cb.count( person ) ).where( cb.equal( personCount.correlate( contacts ).get( "id" ), person.get( "id" ) ) );

			query.multiselect( document.get( "id" ), personCount.getSelection() );

			List<?> l = entityManager.createQuery( query ).getResultList();
			Assertions.assertEquals( 2, l.size() );
		} );
	}
}
