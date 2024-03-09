package org.hibernate.orm.test.locking;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

@RunWith( BytecodeEnhancerRunner.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17828" )
public class LockExistingBytecodeProxyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MainEntity.class,
				ReferencedEntity.class,
		};
	}

	@Test
	public void testFindAndLockAfterFind() {
		inTransaction( session -> {
			final MainEntity main = session.find( MainEntity.class, 1L );
			assertFalse( Hibernate.isInitialized( main.referenced ) );
			final ReferencedEntity lazyEntity = session.find( ReferencedEntity.class, 1L, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockModeType.PESSIMISTIC_WRITE, session.getLockMode( lazyEntity ) );
			assertTrue( Hibernate.isInitialized( main.referenced ) );
			assertSame( lazyEntity, main.referenced );
		} );
	}

	@Test
	public void testLockAfterFind() {
		inTransaction( session -> {
			final MainEntity main = session.find( MainEntity.class, 1L );
			assertFalse( Hibernate.isInitialized( main.referenced ) );
			session.lock( main.referenced, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertEquals( LockModeType.PESSIMISTIC_FORCE_INCREMENT, session.getLockMode( main.referenced ) );
			assertTrue( Hibernate.isInitialized( main.referenced ) );
		} );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			final ReferencedEntity e1 = new ReferencedEntity( 1L, "referenced" );
			session.persist( e1 );
			session.persist( new MainEntity( 1L, e1 ) );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
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
