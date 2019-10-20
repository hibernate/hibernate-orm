package org.hibernate.hql.internal.ast.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-13638")
public class JoinProcessorTest
		extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
				Boolean.TRUE
		);
	}

	@Test
	public void testALotOfInValues() {
		List<Long> values = LongStream.rangeClosed( 1, 10000 ).boxed().collect( Collectors.toList() );

		doInJPA( this::entityManagerFactory, entityManager ->
		{
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Person> cq = cb.createQuery( Person.class );
			Root<Person> root = cq.from( Person.class );
			cq.select( root ).where( root.get( "id" ).in( values ) );
			entityManager.createQuery( cq );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}
}