/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TransactionRequiredException;

import org.hamcrest.MatcherAssert;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hibernate.cfg.TransactionSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10877")
public class NonTransactionalDataAccessTest {

	@BeforeEach
	void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.persist( new MyEntity( "entity" ) ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "true"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void testFlushAllowingOutOfTransactionUpdateOperations(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final MyEntity entity = (MyEntity) session.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			MatcherAssert.assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		} );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "true"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void testNativeQueryAllowingOutOfTransactionUpdateOperations(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			session.createNativeQuery( "delete from MY_ENTITY" ).executeUpdate();
		} );
	}

	@Test
	@ExpectedException(TransactionRequiredException.class)
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "false"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void testNativeQueryDisallowingOutOfTransactionUpdateOperations(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			session.createNativeQuery( "delete from MY_ENTITY" ).executeUpdate();
		} );
	}

	@Test
	@ExpectedException(TransactionRequiredException.class)
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "false"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void testFlushDisallowingOutOfTransactionUpdateOperations(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final MyEntity entity = (MyEntity) session.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			MatcherAssert.assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		} );
	}

	@Test
	@ExpectedException(TransactionRequiredException.class)
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "false"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void testFlushOutOfTransaction(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final MyEntity entity = (MyEntity) session.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			MatcherAssert.assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		} );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = ALLOW_UPDATE_OUTSIDE_TRANSACTION, value = "true"))
	@DomainModel(annotatedClasses = MyEntity.class)
	@SessionFactory
	public void hhh17743Test(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			MyEntity entity = new MyEntity("N1");
			session.persist(entity);

			var q = session.createNamedQuery("deleteByName");
			q.setParameter("name", "N1");
			int d = q.executeUpdate();
			Assertions.assertEquals( 0, d );
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	@NamedQuery(name = "deleteByName", query = "delete from MyEntity where name = :name")
	public static class MyEntity {
		@Id
		@GeneratedValue
		long id;

		String name;

		public MyEntity() {
		}

		public MyEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
