/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositeelement;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/compositeelement/Parent.hbm.xml"
)
@SessionFactory
public class CompositeElementTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testHandSQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child c = new Child( "Child One" );
					Parent p = new Parent( "Parent" );
					p.getChildren().add( c );
					c.setParent( p );
					session.persist( p );
					session.flush();

					p.getChildren().remove( c );
					c.setParent( null );
					session.flush();

					p.getChildren().add( c );
					c.setParent( p );
				}
		);

		Parent parent = scope.fromTransaction( session -> {
					session.createQuery( "select distinct p from Parent p join p.children c where c.name like 'Child%'",
									Parent.class )
							.uniqueResult();
					session.clear();
					session.createQuery(
									"select new Child(c.name) from Parent p left outer join p.children c where c.name like 'Child%'",
									Child.class )
							.uniqueResult();
					session.clear();
					//s.createQuery("select c from Parent p left outer join p.children c where c.name like 'Child%'").uniqueResult(); //we really need to be able to do this!
					session.clear();
					return session.createQuery( "from Parent p left join fetch p.children", Parent.class )
							.uniqueResult();
				}
		);

		scope.inTransaction(
				session ->
						session.remove( parent )
		);
	}

	@Test
	public void testCustomColumnReadAndWrite(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Child c = new Child( "Child One" );
			c.setPosition( 1 );
			Parent p = new Parent( "Parent" );
			p.getChildren().add( c );
			c.setParent( p );
			s.persist( p );
			s.flush();

			// Oracle returns BigDecimaal while other dialects return Integer;
			// casting to Number so it works on all dialects
			Number sqlValue = ((Number) s.createNativeQuery(
							"select child_position from ParentChild c where c.name='Child One'" )
					.uniqueResult());
			assertThat( sqlValue.intValue() ).isEqualTo( 0 );

			Integer hqlValue = s.createQuery(
							"select c.position from Parent p join p.children c where p.name='Parent'", Integer.class )
					.uniqueResult();
			assertThat( hqlValue ).isEqualTo( 1 );

//			p = (Parent) s.createCriteria( Parent.class ).add( Restrictions.eq( "name", "Parent" ) ).uniqueResult();

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
			Root<Parent> root = criteria.from( Parent.class );
			criteria.where( criteriaBuilder.equal( root.get( "name" ), "Parent" ) );

			p = s.createQuery( criteria ).uniqueResult();

			c = p.getChildren().iterator().next();
			assertThat( c.getPosition() ).isEqualTo( 1 );

			p = s.createQuery( "from Parent p join p.children c where c.position = 1", Parent.class ).uniqueResult();
			c = p.getChildren().iterator().next();
			assertThat( c.getPosition() ).isEqualTo( 1 );

			c.setPosition( 2 );
			s.flush();
			sqlValue = ((Number) s.createNativeQuery(
							"select child_position from ParentChild c where c.name='Child One'" )
					.uniqueResult());
			assertThat( sqlValue.intValue() ).isEqualTo( 1 );
			s.remove( p );
		} );
	}

}
