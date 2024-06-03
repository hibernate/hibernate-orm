package org.hibernate.orm.test.locking;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@DomainModel(
		annotatedClasses = {
				LockExistingBytecodeProxyTest.MainEntity.class,
				LockExistingBytecodeProxyTest.ReferencedEntity.class,
		}
)
@SessionFactory
@BytecodeEnhanced
@Jira( "https://hibernate.atlassian.net/browse/HHH-17828" )
public class LockExistingBytecodeProxyTest {

	@Test
	public void testFindAndLockAfterFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MainEntity main = session.find( MainEntity.class, 1L );
			assertFalse( Hibernate.isInitialized( main.referenced ) );
			final ReferencedEntity lazyEntity = session.find( ReferencedEntity.class, 1L, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockModeType.PESSIMISTIC_WRITE, session.getLockMode( lazyEntity ) );
			assertTrue( Hibernate.isInitialized( main.referenced ) );
			assertSame( lazyEntity, main.referenced );
		} );
	}

	@Test
	public void testLockAfterFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MainEntity main = session.find( MainEntity.class, 1L );
			assertFalse( Hibernate.isInitialized( main.referenced ) );
			session.lock( main.referenced, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertEquals( LockModeType.PESSIMISTIC_FORCE_INCREMENT, session.getLockMode( main.referenced ) );
			assertTrue( Hibernate.isInitialized( main.referenced ) );
		} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ReferencedEntity e1 = new ReferencedEntity( 1L, "referenced" );
			session.persist( e1 );
			session.persist( new MainEntity( 1L, e1 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MainEntity" ).executeUpdate();
			session.createMutationQuery( "delete from ReferencedEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "MainEntity" )
	public static class MainEntity {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private ReferencedEntity referenced;

		protected MainEntity() {
		}

		public MainEntity(long id, ReferencedEntity referenced) {
			this.id = id;
			this.referenced = referenced;
		}

		public ReferencedEntity getReferenced() {
			return referenced;
		}
	}

	@Entity( name = "ReferencedEntity" )
	public static class ReferencedEntity {
		@Id
		private Long id;

		@Version
		private Long version;

		private String name;

		protected ReferencedEntity() {
		}

		public ReferencedEntity(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public long getVersion() {
			return version;
		}
	}
}
