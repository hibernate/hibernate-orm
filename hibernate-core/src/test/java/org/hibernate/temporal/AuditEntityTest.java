/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SessionFactory
@DomainModel(annotatedClasses = AuditEntityTest.AuditEntity.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSACTION_ID_SUPPLIER,
		value = "org.hibernate.temporal.AuditEntityTest$TxIdSupplier"))
class AuditEntityTest {
	private static int currentTxId;

	public static class TxIdSupplier implements Supplier<Integer> {
		@Override
		public Integer get() {
			return ++currentTxId;
		}
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = new AuditEntity();
					entity.id = 1L;
					entity.text = "hello";
					entity.stringSet.add( "hello" );
					session.persist( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					entity.text = "goodbye";
					entity.stringSet.add( "goodbye" );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					assertNull( entity );
				}
		);
		try ( var s = scope.getSessionFactory().withOptions().atTransaction(0).open() ) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
		try ( var s = scope.getSessionFactory().withOptions().atTransaction(1).open() ) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertEquals( "hello", entity.text);
			assertEquals( Set.of("hello"), entity.stringSet);
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertSame( entity, result );
		}
		try ( var s = scope.getSessionFactory().withOptions().atTransaction(2).open() ) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertEquals( "goodbye", entity.text);
			assertEquals( Set.of("hello","goodbye"), entity.stringSet );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertSame( entity, result );
		}
		try ( var s = scope.getSessionFactory().withOptions().atTransaction(3).open() ) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
		try ( var s = scope.getSessionFactory().withOptions().atTransaction(4).open() ) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
	}
	@Audited
	@Entity(name = "AuditEntity")
	static class AuditEntity {
		@Id
		long id;
		String text;
		@Version
		int version;
		@Audited
		@ElementCollection
		Set<String> stringSet = new HashSet<>();
	}
}
