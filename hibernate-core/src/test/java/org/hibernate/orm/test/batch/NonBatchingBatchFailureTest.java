/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Shawn Clowater
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-7689")
@DomainModel(
		annotatedClasses = {
				NonBatchingBatchFailureTest.User.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "-1"),
				@Setting(name = AvailableSettings.CHECK_NULLABILITY, value = "false")
		}
)
public class NonBatchingBatchFailureTest {

	@Test
	public void testBasicInsertion(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();

					try {
						session.persist( new User( 1, "ok" ) );
						session.persist( new User( 2, null ) );
						session.persist( new User( 3, "ok" ) );
						// the flush should fail
						session.flush();
						fail( "Expecting failed flush" );
					}
					catch (Exception expected) {
						try {
							//at this point the transaction is still active but the batch should have been aborted (have to use reflection to get at the field)
							Field field = session.getJdbcCoordinator().getClass().getDeclaredField( "currentBatch" );
							field.setAccessible( true );
							Batch batch = (Batch) field.get( session.getJdbcCoordinator() );
							assertThat( batch ).isNull();
						}
						catch (Exception fieldException) {
							fail( "Couldn't inspect field " + fieldException.getMessage() );
						}
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "`USER`")
	public static class User {
		private Integer id;
		private String name;

		public User() {
		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Column(nullable = false)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
