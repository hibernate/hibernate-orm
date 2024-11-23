/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import java.sql.Statement;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestForIssue(jiraKey = "HHH-12445")
public class JoinedNullNotNullDiscriminatorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				RootEntity.class,
				Val1Entity.class,
				Val2Entity.class,
				NotNullEntity.class
		};
	}

	@Test
	public void test() {
		inTransaction( session -> {
			Val1Entity val1 = new Val1Entity();
			val1.setId( 1L );

			Val2Entity val2 = new Val2Entity();
			val2.setId( 2L );

			RootEntity root = new RootEntity();
			root.setId( 3L );

			session.persist( val1 );
			session.persist( val2 );
			session.persist( root );

			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate(
							"insert into root_ent (DTYPE, id) " +
									"values ('other', 4)"
					);
				}
			} );
		} );

		inTransaction( session -> {
			Map<Long, RootEntity> entities = session.createQuery(
					"select e from root_ent e", RootEntity.class )
					.getResultList()
					.stream()
					.collect( Collectors.toMap( RootEntity::getId, Function.identity() ) );

			assertThat( entities ).extractingByKey( 1L ).isInstanceOf( Val1Entity.class );
			assertThat( entities ).extractingByKey( 2L ).isInstanceOf( Val2Entity.class );
			assertThat( entities ).extractingByKey( 3L ).isInstanceOf( RootEntity.class );
			assertThat( entities ).extractingByKey( 4L ).isInstanceOf( NotNullEntity.class );
		} );
	}

	@Entity(name = "root_ent")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn()
	@DiscriminatorValue("null")
	public static class RootEntity {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "val1_ent")
	@DiscriminatorValue("val1")
	public static class Val1Entity extends RootEntity {

	}

	@Entity(name = "val2_ent")
	@DiscriminatorValue("val2")
	public static class Val2Entity extends RootEntity {

	}

	@Entity(name = "notnull_ent")
	@DiscriminatorValue("not null")
	public static class NotNullEntity extends RootEntity {

	}
}
