package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-20971")
@DomainModel(annotatedClasses = StaleReturningUpdateTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = USE_GET_GENERATED_KEYS, value = "false"))
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
class StaleReturningUpdateTest {
	@Entity
	@Table(name = "stale_returning_update_item")
	static class Item {
		@Id int id;
		@Version int version;
		int quantity;
		@Generated(event = UPDATE, sql = "42") Integer answer;
	}

	@Test
	void statelessUpdateWithStaleVersion(SessionFactoryScope scope) {
		var stale = new Item();
		scope.inTransaction( session -> session.persist( stale ) );
		scope.inTransaction( session -> session.find( Item.class, stale.id ).quantity++ );
		assertThrows( OptimisticLockException.class,
				() -> scope.inStatelessTransaction( session -> session.update( stale ) ) );
	}
}
