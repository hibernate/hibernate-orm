/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author lrobot.qq@gmail.com
 * how to run this test case: ./gradlew :hibernate-core:test --tests '*DiscriminatorSuperClassTest'
 */
@JiraKey(value = "HHH-18282")
@DomainModel(
		annotatedClasses = {
			SingleTableNotNullDiscriminatorSuperClassTest.RootEntity.class,
			SingleTableNotNullDiscriminatorSuperClassTest.Val1Entity.class,
			SingleTableNotNullDiscriminatorSuperClassTest.Val2Entity.class,
			SingleTableNotNullDiscriminatorSuperClassTest.OtherEntity.class,
			SingleTableNotNullDiscriminatorSuperClassTest.NotNullEntity.class
		}
)
@SessionFactory
public class SingleTableNotNullDiscriminatorSuperClassTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Val1Entity val1 = new Val1Entity();
			val1.setId( 1L );

			Val2Entity val2 = new Val2Entity();
			val2.setId( 2L );

			RootEntity root = new RootEntity();
			root.setId( 3L );

			OtherEntity otherEntity = new OtherEntity();
			otherEntity.setId( 4L );

			session.persist( val1 );
			session.persist( val2 );
			session.persist( root );
			session.persist( otherEntity );

		} );

		scope.inTransaction( session -> {
			// can ref more from spring-data: SimpleJpaRepository
			final CriteriaQuery<NotNullEntity> criteriaQuery = session.getCriteriaBuilder().createQuery( NotNullEntity.class );
			final Root<NotNullEntity> root = criteriaQuery.from( NotNullEntity.class );
			criteriaQuery.select( root );

			Map<Long, NotNullEntity> entities = session.createQuery( criteriaQuery ).getResultList()
					.stream()
					.collect( Collectors.toMap( NotNullEntity::getId, Function.identity() ) );
			assertThat( entities ).hasSize( 3 );  //select NotNullEntity and all it subclasses only, not select RootEntity
			assertThat( entities ).extractingByKey( 1L ).isInstanceOf( Val1Entity.class );
			assertThat( entities ).extractingByKey( 2L ).isInstanceOf( Val2Entity.class );
			// assertThat( entities ).extractingByKey( 3L ).isInstanceOf( RootEntity.class );
			assertThat( entities ).extractingByKey( 4L ).isInstanceOf( NotNullEntity.class );
		} );
	}

	@Entity(name = "root_ent")
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
	public static class Val1Entity extends NotNullEntity {

	}

	@Entity(name = "val2_ent")
	@DiscriminatorValue("val2")
	public static class Val2Entity extends NotNullEntity {

	}

	@Entity(name = "other_ent")
	@DiscriminatorValue("other")
	public static class OtherEntity extends NotNullEntity {
	}

	@Entity(name = "notnull_ent")
	@DiscriminatorValue("not null")
	public static class NotNullEntity extends RootEntity {

	}
}
