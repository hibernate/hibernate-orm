package org.hibernate.orm.test.query.criteria;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel( annotatedClasses = {
		CriteriaTypeValidationTests.Parent.class,
		CriteriaTypeValidationTests.Child.class
} )
@SessionFactory
public class CriteriaTypeValidationTests {

	@Test
	@JiraKey( "HHH-17001" )
	public void testHhh17001(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Child someChildToCompareTo = new Child( 1L );
					session.persist(someChildToCompareTo);

					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					final Root<Parent> root = cq.from(Parent.class);
					final SetJoin<Parent, Child> childrenJoin = root.joinSet( "children" );
					// This will cause a StackOverflowError:
					childrenJoin.on(cb.equal(childrenJoin, someChildToCompareTo));
					// This works:
					// cq.where(cb.equal(childrenJoin, someChildToCompareTo));
					session.createQuery(cq).getResultList();
				}
		);
	}

	@Test
	@JiraKey( "HHH-20313" )
	public void testCompareDoubleToInteger(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					final Root<Parent> root = cq.from(Parent.class);
					cq.where( cb.lt( root.get( "price" ), cb.prod( cb.literal( 54 ), 2 ) ) );
					session.createQuery(cq).getResultList();
				}
		);
	}

	@Test
	@JiraKey( "HHH-18831" )
	public void testSubquerySelectionIncompatibleWithResultType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Parent> cq = cb.createQuery( Parent.class );

					// subquery declared to return the entity, but selecting its id
					final Subquery<Parent> subquery = cq.subquery( Parent.class );
					final Root<Parent> subRoot = subquery.from( Parent.class );
					assertThrows(
							IllegalArgumentException.class,
							() -> subquery.select( subRoot.get( "id" ) )
					);
				}
		);
	}

	@Test
	@JiraKey( "HHH-18831" )
	public void testSubquerySelectionCompatibleWithResultType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					// entity compared to a subquery selecting the entity
					final CriteriaQuery<Parent> entityQuery = cb.createQuery( Parent.class );
					final Root<Parent> entityRoot = entityQuery.from( Parent.class );
					final Subquery<Parent> entitySubquery = entityQuery.subquery( Parent.class );
					entitySubquery.select( entitySubquery.from( Parent.class ) );
					entityQuery.where( cb.in( entityRoot ).value( entitySubquery ) );
					session.createQuery( entityQuery ).getResultList();

					// id compared to a subquery selecting the id
					final CriteriaQuery<Parent> idQuery = cb.createQuery( Parent.class );
					final Root<Parent> idRoot = idQuery.from( Parent.class );
					final Subquery<Long> idSubquery = idQuery.subquery( Long.class );
					idSubquery.select( idSubquery.from( Parent.class ).get( "id" ) );
					idQuery.where( cb.in( idRoot.<Long>get( "id" ) ).value( idSubquery ) );
					session.createQuery( idQuery ).getResultList();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;
		private Double price;

		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}
	}
}
