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
import org.hibernate.query.criteria.JpaCriteriaUpdate;
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

@ServiceRegistry
@DomainModel(standardModels = { StandardDomainModel.GAMBIT, StandardDomainModel.CONTACTS })
@SessionFactory
@JiraKey("HHH-16138")
public class UpdateJoinTests {

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
	public void testUpdateWithJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//tag::hql-update-join-example[]
					int updated = session.createMutationQuery(
							"update BasicEntity b left join Contact c on b.id = c.id " +
									"set b.data = c.name.first " +
									"where c.id is not null"
					).executeUpdate();
					//end::hql-update-join-example[]
					assertEquals( 1, updated );
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "A", basicEntity.getData() );
				}
		);
	}

	@Test
	public void testUpdateWithJoinCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaUpdate<BasicEntity> criteriaUpdate = cb.createCriteriaUpdate( BasicEntity.class );
					final JpaRoot<BasicEntity> b = criteriaUpdate.from( BasicEntity.class );
					final JpaEntityJoin<BasicEntity,Contact> c = b.join( Contact.class, JoinType.LEFT );
					c.on( b.get( "id" ).equalTo( c.get( "id" ) ) );
					criteriaUpdate.set( b.<String>get( "data" ), c.get( "name" ).get( "first" ) );
					criteriaUpdate.where( c.get( "id" ).isNotNull() );
					int updated = session.createMutationQuery( criteriaUpdate ).executeUpdate();
					assertEquals( 1, updated );
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "A", basicEntity.getData() );
				}
		);
	}
}
