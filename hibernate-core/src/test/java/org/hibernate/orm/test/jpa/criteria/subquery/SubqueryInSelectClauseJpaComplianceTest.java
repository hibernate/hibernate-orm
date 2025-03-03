/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.Map;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey(value = "HHH-13111")
public class SubqueryInSelectClauseJpaComplianceTest extends AbstractSubqueryInSelectClauseTest {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_QUERY_COMPLIANCE, true );
	}

	@Test(expected = IllegalStateException.class)
	public void testSubqueryInSelectClause() {
		doInJPA( this::entityManagerFactory, em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Document> document = query.from( Document.class );
			Join<?, ?> contacts = document.join( "contacts", JoinType.LEFT );

			Subquery<Long> personCount = query.subquery( Long.class );
			Root<Person> person = personCount.from( Person.class );
			personCount.select( cb.count( person ) ).where( cb.equal( contacts.get( "id" ), person.get( "id" ) ) );

			query.multiselect( document.get( "id" ), personCount.getSelection() );

			em.createQuery( query ).getResultList();
		} );
	}
}
