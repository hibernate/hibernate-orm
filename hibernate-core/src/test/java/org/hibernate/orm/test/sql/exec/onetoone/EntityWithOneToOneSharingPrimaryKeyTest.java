/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneSharingPrimaryKey;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithOneToOneSharingPrimaryKey.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
@FailureExpected
public class EntityWithOneToOneSharingPrimaryKeyTest {

	@Test
	public void testOperations(SessionFactoryScope scope) {

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		EntityWithOneToOneSharingPrimaryKey entity = new EntityWithOneToOneSharingPrimaryKey(
				other.getId(),
				"first",
				Integer.MAX_VALUE
		);

		entity.setOther( other );

		scope.inTransaction(

				session -> {
					session.save( other );
					session.save( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithOneToOneSharingPrimaryKey loaded = session.get(
							EntityWithOneToOneSharingPrimaryKey.class,
							2
					);
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					final SimpleEntity loaded = session.get(
							SimpleEntity.class,
							2
					);
					assert loaded != null;
				}
		);

		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOneSharingPrimaryKey e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}
}
