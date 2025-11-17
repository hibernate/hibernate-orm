/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.PreparedStatement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;

import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the correctness of the parameter hibernate.dialect.hana.use_legacy_boolean_type which controls the mapping of
 * boolean types to be either TINYINT (parameter is set to true) or BOOLEAN (default behavior or parameter is set to
 * false)
 *
 * @author Jonathan Bregler
 */
@RequiresDialect(HANADialect.class)
@DomainModel(annotatedClasses = {HANABooleanTest.BooleanEntity.class, HANABooleanTest.LegacyBooleanEntity.class})
public class HANABooleanTest {

	private static final String ENTITY_NAME = "BooleanEntity";
	private static final String LEGACY_ENTITY_NAME = "LegacyBooleanEntity";

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE COLUMN TABLE " + ENTITY_NAME + " (key INTEGER, bool BOOLEAN, PRIMARY KEY (key))" ) ) {
					ps.execute();
				}

				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE COLUMN TABLE " + LEGACY_ENTITY_NAME + " (key INTEGER, bool TINYINT, PRIMARY KEY (key))" ) ) {
					ps.execute();
				}
			} );
		} );
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection.prepareStatement( "DROP TABLE " + ENTITY_NAME ) ) {
					ps.execute();
				}
				catch (Exception e) {
					// Ignore
				}

				try ( PreparedStatement ps = connection.prepareStatement( "DROP TABLE " + LEGACY_ENTITY_NAME ) ) {
					ps.execute();
				}
				catch (Exception e) {
					// Ignore
				}
			} );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settings = {
			@Setting( name = "hibernate.dialect.hana.use_legacy_boolean_type", value = "false")
	})
	public void testBooleanType(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			BooleanEntity entity = new BooleanEntity();
			entity.key = 1;
			entity.bool = Boolean.TRUE;

			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			Query<BooleanEntity> query = session.createQuery( "select b from " + ENTITY_NAME + " b where bool = true", BooleanEntity.class );
			BooleanEntity retrievedEntity = query.getSingleResult();

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertTrue( retrievedEntity.bool );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@SessionFactory(exportSchema = false)
	public void testBooleanTypeDefaultBehavior(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			BooleanEntity entity = new BooleanEntity();
			entity.key = 1;
			entity.bool = Boolean.TRUE;

			session.persist( entity );
		} );

		scope.inTransaction(  session -> {
			Query<BooleanEntity> query = session.createQuery( "select b from " + ENTITY_NAME + " b where bool = true", BooleanEntity.class );
			BooleanEntity retrievedEntity = query.getSingleResult();

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertTrue( retrievedEntity.bool );
		} );
	}

	@Test/*(expected = PersistenceException.class)*/
	@JiraKey(value = "HHH-12132")
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settings = {
			@Setting( name = "hibernate.dialect.hana.use_legacy_boolean_type", value = "false")
	})
	public void testLegacyBooleanType(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(  session -> {
			LegacyBooleanEntity entity = new LegacyBooleanEntity();
			entity.key = 2;
			entity.bool = Boolean.FALSE;

			session.persist( entity );
		} );

		scope.inTransaction(   session -> {
			Query<LegacyBooleanEntity> query = session.createQuery( "select b from " + LEGACY_ENTITY_NAME + " b where bool = true", LegacyBooleanEntity.class );
			Assertions.assertThrows( PersistenceException.class, () -> query.getSingleResult() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settings = {
			@Setting( name = "hibernate.dialect.hana.use_legacy_boolean_type", value = "true")
	})
	public void testLegacyBooleanTypeLegacyBehavior(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(  session -> {
			LegacyBooleanEntity entity = new LegacyBooleanEntity();
			entity.key = 1;
			entity.bool = Boolean.TRUE;

			session.persist( entity );
		} );

		scope.inTransaction(   session -> {
			Query<LegacyBooleanEntity> query = session.createQuery( "select b from " + LEGACY_ENTITY_NAME + " b where bool = true", LegacyBooleanEntity.class );
			LegacyBooleanEntity retrievedEntity = query.getSingleResult();

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertTrue( retrievedEntity.bool );
		} );
	}

	@Test/*(expected = PersistenceException.class)*/
	@JiraKey(value = "HHH-12132")
	@SessionFactory(exportSchema = false)
	@ServiceRegistry(settings = {
			@Setting( name = "hibernate.dialect.hana.use_legacy_boolean_type", value = "true")
	})
	public void testBooleanTypeLegacyBehavior(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(  session -> {
			BooleanEntity entity = new BooleanEntity();
			entity.key = 2;
			entity.bool = Boolean.FALSE;

			session.persist( entity );
		} );

		scope.inTransaction(   session -> {
			Query<BooleanEntity> query = session.createQuery( "select b from " + ENTITY_NAME + " b where bool = true", BooleanEntity.class );
			Assertions.assertThrows( PersistenceException.class, () -> query.getSingleResult() );
		} );
	}


	@Entity(name = LEGACY_ENTITY_NAME)
	public static class LegacyBooleanEntity {

		@Id
		public Integer key;

		public Boolean bool;
	}

	@Entity(name = ENTITY_NAME)
	public static class BooleanEntity {

		@Id
		public Integer key;

		public Boolean bool;
	}

}
