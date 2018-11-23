/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.subquery;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-13111")
public class SubqueryInSelectClauseTest extends AbstractSubqueryInSelectClauseTest {

	@Test
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

			List<?> l = em.createQuery( query ).getResultList();
			assertEquals( 2, l.size() );
		} );
	}
}
