/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import java.sql.Statement;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-12445")
@DomainModel(
		annotatedClasses = {
				JoinedNullNotNullDiscriminatorTest.RootEntity.class,
				JoinedNullNotNullDiscriminatorTest.Val1Entity.class,
				JoinedNullNotNullDiscriminatorTest.Val2Entity.class,
				JoinedNullNotNullDiscriminatorTest.NotNullEntity.class
		}
)
@SessionFactory
public class JoinedNullNotNullDiscriminatorTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

		scope.inTransaction( session -> {
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

		private String name;

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
