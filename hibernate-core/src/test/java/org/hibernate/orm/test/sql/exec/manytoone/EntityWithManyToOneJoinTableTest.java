/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.Calendar;

import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneJoinTable;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithManyToOneJoinTable.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class EntityWithManyToOneJoinTableTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					session.delete( loaded );
					session.delete( loaded.getOther() );
				}
		);
	}

	@Test
	public void testSave(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.save( other );
		} );
		scope.inTransaction( session -> {
			session.save( entity );
		} );

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );

		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );

		SimpleEntity anOther = new SimpleEntity(
				3,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE,
				Long.MIN_VALUE,
				null
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					assert loaded != null;
					session.save( anOther );
					loaded.setOther( anOther );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );

					assertThat( loaded.getOther(), notNullValue() );
					assertThat( loaded.getOther().getId(), equalTo( 3 ) );
				}
		);
	}
}
