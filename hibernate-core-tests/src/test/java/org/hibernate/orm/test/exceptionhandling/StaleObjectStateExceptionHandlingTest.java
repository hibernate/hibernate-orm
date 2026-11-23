package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey("HHH-12666")
@RequiresDialect(H2Dialect.class)
public class StaleObjectStateExceptionHandlingTest extends BaseExceptionHandlingTest {

	public StaleObjectStateExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class };
	}

	@AfterEach
	void tearDown() {
		//noinspection resource
		sessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testStaleObjectMerged() {
		var created = TransactionUtil.fromTransaction( sessionFactory(), (s) -> {
			A a = new A();
			a.id = 1;
			s.persist( a );
			return a;
		} );

		var detached = TransactionUtil.fromTransaction( sessionFactory(), (s) -> {
			A aGet = s.find( A.class, 1 );
			aGet.name = "A. Name";
			return aGet;
		} );


		created.name = "Another Name";

		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				s.merge( created );
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onStaleObjectMergeAndUpdateFlush( expected );
			}
		} );
	}

	@Test
	public void testStaleObjectUpdatedAndFlushed() {
		var created = TransactionUtil.fromTransaction(  sessionFactory(), (s) -> {
			A a = new A();
			a.id = 2;
			s.persist( a );
			return a;
		} );

		var detached = TransactionUtil.fromTransaction( sessionFactory(), (s) -> {
			A aGet = s.find( A.class, 2 );
			aGet.name = "A. Name";
			return aGet;
		} );

		created.name = "Another Name";

		TransactionUtil.inTransaction( sessionFactory(), (s) -> {
			try {
				s.merge( created );
				s.flush();
				fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected) {
				exceptionExpectations.onStaleObjectMergeAndUpdateFlush( expected );
			}
		} );
	}

	@Test
	@JiraKey("HHH-15410")
	void testConversionDoesNotLoadEntity() {
		sessionFactory().inTransaction( session -> {
			final var entity = new A();
			entity.id = 1L;
			session.persist( entity );
		} );
		final var stale = new StaleObjectStateException( A.class.getName(), 1L );
		final var inspector = new SQLStatementInspector();
		try ( var session = sessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var converted = session.getExceptionConverter().convert( stale );
			assertThat( converted ).hasCause( stale ).isInstanceOfSatisfying( OptimisticLockException.class,
					e -> assertThat( e.getEntity() ).isNull() );
			inspector.assertExecutedCount( 0 );
		}
	}

	@SuppressWarnings("unused")
	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		private String name;

		@Version
		@Column(name = "ver")
		private int version;
	}
}
