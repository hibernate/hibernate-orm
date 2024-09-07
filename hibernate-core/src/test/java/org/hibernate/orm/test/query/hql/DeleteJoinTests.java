/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.time.LocalDate;

import org.hibernate.query.common.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.Contact.Name;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ServiceRegistry
@DomainModel(standardModels = { StandardDomainModel.GAMBIT, StandardDomainModel.CONTACTS })
@SessionFactory
@JiraKey("HHH-16138")
public class DeleteJoinTests {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Contact(
							1,
							new Name( "A", "B" ),
							Contact.Gender.FEMALE,
							LocalDate.of( 2000, 1, 1 )
					) );
					session.persist( new BasicEntity( 1, "data" ) );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Contact" ).executeUpdate();
			session.createMutationQuery( "delete from BasicEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testDeleteWithJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//tag::hql-delete-join-example[]
					int updated = session.createMutationQuery(
							"delete from BasicEntity b left join Contact c on b.id = c.id " +
									"where c.id is not null"
					).executeUpdate();
					//end::hql-delete-join-example[]
					assertEquals( 1, updated );
					assertNull( session.find( BasicEntity.class, 1 ) );
				}
		);
	}

	@Test
	public void testDeleteWithJoinCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaDelete<BasicEntity> criteriaDelete = cb.createCriteriaDelete( BasicEntity.class );
					final JpaRoot<BasicEntity> b = criteriaDelete.from( BasicEntity.class );
					final JpaEntityJoin<BasicEntity, Contact> c = b.join( Contact.class, JoinType.LEFT );
					c.on( b.get( "id" ).equalTo( c.get( "id" ) ) );
					criteriaDelete.where( c.get( "id" ).isNotNull() );
					int updated = session.createMutationQuery( criteriaDelete ).executeUpdate();
					assertEquals( 1, updated );
					assertNull( session.find( BasicEntity.class, 1 ) );
				}
		);
	}
}
