package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {StatelessSessionVersioningTest.IdentityVersioned.class,
		StatelessSessionVersioningTest.UUIDVersioned.class})
public class StatelessSessionVersioningTest {
	@Test void testIdentity(SessionFactoryScope scope) {
		Dialect dialect = scope.getMetadataImplementor().getDatabase().getDialect();
		scope.inStatelessTransaction(s -> {
			IdentityVersioned v = new IdentityVersioned();
			s.insert(v);
			assertEquals(0, v.version);
			s.update(v);
			assertEquals(1, v.version);
			if ( !(dialect instanceof SQLServerDialect) && !(dialect instanceof OracleDialect) ) {
				//TODO: upsert() with IDENTITY not working on SQL Server
				//TODO: upsert() with version not working on Oracle
				s.upsert(v);
				assertEquals(2, v.version);
			}
			s.delete(v);
		});
	}
	@Test void testUUID(SessionFactoryScope scope) {
		Dialect dialect = scope.getMetadataImplementor().getDatabase().getDialect();
		scope.inStatelessTransaction(s -> {
			UUIDVersioned v = new UUIDVersioned();
			s.insert(v);
			assertEquals(0, v.version);
			s.update(v);
			assertEquals(1, v.version);
			if ( !(dialect instanceof OracleDialect) ) {
				//TODO: upsert() with version not working on Oracle
				s.upsert(v);
				assertEquals(2, v.version);
			}
			s.delete(v);
		});
	}
	@Entity
	static class IdentityVersioned {
		@Id @GeneratedValue(strategy = IDENTITY)
		long id;
		@Version
		int version = -1;
	}
	@Entity
	static class UUIDVersioned {
		@Id @GeneratedValue(strategy = GenerationType.UUID)
		UUID id;
		@Version
		int version = -1;
	}
}
