package org.hibernate.orm.test.query.hql;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.orm.test.query.sqm.domain.Person;
import org.hibernate.orm.test.query.sqm.domain.Person_;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-17416")
public class HHH17416Test extends BaseSessionFactoryFunctionalTest {

	private static final Person person = new Person();
	static {
		person.setPk(7);
		person.setNickName("Tadpole");
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Person.class};
	}

	@BeforeEach
	public void setup() {
		inTransaction(session -> session.persist(person));
	}

	@AfterEach
	public void teardown() {
		inTransaction(session -> session.createMutationQuery("delete from Person").executeUpdate());
	}

	@Test
	public void testWhereClauseWithTuple() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					SelectionQuery<Person> selectionQuery = entityManager.createSelectionQuery("from Person p where (p.id, p.nickName) = (:val1, :val2)", Person.class);
					selectionQuery = selectionQuery.setParameter("val1", person.getPk()).setParameter("val2", person.getNickName());
					Person retrievedPerson = selectionQuery.getSingleResult();
					Assertions.assertEquals(person.getPk(), retrievedPerson.getPk());
					Assertions.assertEquals(person.getNickName(), retrievedPerson.getNickName());
				}
		);
	}

	@Test
	public void testWhereClauseWithInvalidObjectType() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
					Root<Person> root = criteria.from(Person.class);
					criteria.select(root);

					try {
						criteria.where(builder.equal(root.get(Person_.nickName), builder.literal(new Object())));
						TypedQuery<Person> ignored = entityManager.createQuery(criteria);
						Assertions.fail("Should have failed with 'Cannot compare left expression of type' of type `org.hibernate.query.SemanticException'");
					}
					catch (Exception e) {
						Assertions.assertTrue(e instanceof SemanticException);
						Assertions.assertTrue(e.getMessage().startsWith("Cannot compare left expression of type"));
					}
				}
		);
	}
}
