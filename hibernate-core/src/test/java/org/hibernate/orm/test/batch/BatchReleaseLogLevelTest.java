package org.hibernate.orm.test.batch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {
				BatchReleaseLogLevelTest.Product.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"),
				@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
		}
)
@MessageKeyInspection(
		messageKey = "HHH100503",
		logger = @Logger(loggerName = "org.hibernate.orm.jdbc.batch")
)
@JiraKey(value = "HHH-20861")
public class BatchReleaseLogLevelTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncate() );
	}

	@Test
	public void testBatchReleaseLoggedAtInfoLevelAfterConstraintViolation(
			SessionFactoryScope scope,
			MessageKeyWatcher watcher) {
		scope.inTransaction( session -> {
			Product product = new Product( 1L, "PRODUCT-001" );
			session.persist( product );
		} );


		assertThrows( Exception.class, () -> {
					scope.inTransaction( session -> {
						session.persist( new Product( 2L, "PRODUCT-002" ) );
						// This will cause a constraint violation when flushed
						session.persist( new Product( 3L, "PRODUCT-001" ) );
					} );
				}
		);

		assertFalse(
				watcher.wasTriggered(),
				"HHH100503 should have been logged during session close after constraint violation: " + watcher.getTriggeredMessages()
		);
	}

	@Test
	public void testBatchReleaseLoggedDuringExplicitSessionClose(
			SessionFactoryScope scope,
			MessageKeyWatcher watcher) {
		// Set up the constraint violation scenario
		scope.inTransaction( session -> {
			Product product = new Product( 10L, "PRODUCT-010" );
			session.persist( product );
		} );

		scope.inSession( session -> {
			session.beginTransaction();
			try {
				Product duplicate = new Product( 20L, "PRODUCT-010" );
				// Now try to persist a duplicate product
				session.persist( duplicate );
				// Force the batch execution which will cause a constraint violation
				session.flush();
				fail( "Expected PersistenceException" );
			}
			catch (PersistenceException expected) {
				// Application catches the exception and rolls back
				session.getTransaction().rollback();
			}
			// Session will be closed by inSession(), which triggers the log message
		} );

		assertFalse(
				watcher.wasTriggered(),
				"HHH100503 should have been logged when session was closed: " + watcher.getTriggeredMessages()
		);
	}

	@Entity(name = "Product")
	@Table(
			name = "product",
			uniqueConstraints = @UniqueConstraint(columnNames = "code")
	)
	@SecondaryTable(name = "product_details")
	public static class Product {
		@Id
		private Long id;

		@Column(nullable = false)
		private String code;

		@Column(table = "product_details")
		private String description;

		@Column(table = "product_details")
		private String category;

		protected Product() {
		}

		public Product(Long id, String code) {
			this.id = id;
			this.code = code;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}
	}
}
