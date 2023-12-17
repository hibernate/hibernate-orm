package org.hibernate.orm.test.bytecode.enhancement.superclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.time.LocalDateTime;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@JiraKey("HHH-17418")
@RunWith(BytecodeEnhancerRunner.class)
public class MappedSuperclassTest extends BaseCoreFunctionalTestCase {
	private static final LocalDateTime TEST_DATE_UPDATED_VALUE = LocalDateTime.of( 2023, 11, 10, 0, 0 );
	private static final long TEST_ID = 1L;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			MyEntity testEntity = new MyEntity();
			testEntity.id = TEST_ID;
			s.persist( testEntity );
		} );
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			MyEntity testEntity = s.get( MyEntity.class, TEST_ID );
			assertThat( testEntity.value() ).isEqualTo( TEST_DATE_UPDATED_VALUE );
		} );
	}

	@After
	public void cleanup() {
		doInHibernate( this::sessionFactory, s -> {
			MyEntity testEntity = s.get( MyEntity.class, TEST_ID );
			s.remove( testEntity );
		} );
	}


	@MappedSuperclass
	public static class MappedBase {
		// field is private on purpose so that enhancer will not use field access
		@Column
		private final LocalDateTime updated = TEST_DATE_UPDATED_VALUE;

		public LocalDateTime value() {
			return updated;
		}
	}

	@Entity
	public static class MyEntity extends MappedBase {
		@Id
		Long id;
	}
}
