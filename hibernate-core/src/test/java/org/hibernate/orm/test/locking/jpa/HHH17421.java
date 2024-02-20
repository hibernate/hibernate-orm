package org.hibernate.orm.test.locking.jpa;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;

@DomainModel(annotatedClasses = { Department.class })
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialect(value = SQLServerDialect.class)
public class HHH17421 {

	@Test
	@JiraKey("HHH-17421")
	@FailureExpected
	@Timeout(value = 2, unit=TimeUnit.MINUTES)
	public void testNoFollowonLocking(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inSession( (s) -> {
			scope.inTransaction(
					s,
					session -> {
						final Department engineering = new Department( 1, "Engineering" );
						session.persist( engineering );
					}
			);

			scope.inTransaction(
					s,
					session -> {
						statementInspector.clear();

						final QueryImplementor<Department> query = session.createQuery(
								"select distinct d from Department d",
								Department.class
						);
						query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
						query.list();

						// The only statement should be the initial SELECT .. WITH (UPDLOCK, ..) ..
						// and without any follow-on locking.
						statementInspector.assertExecutedCount( 1 );
					}
			);
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Department" ).executeUpdate();
		} );
	}
}