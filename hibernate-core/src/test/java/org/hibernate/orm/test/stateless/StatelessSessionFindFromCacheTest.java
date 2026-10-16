package org.hibernate.orm.test.stateless;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = StatelessSessionFindFromCacheTest.CachedRecord.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-21000")
public class StatelessSessionFindFromCacheTest {

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> s.insert( new CachedRecord( 1L, "hello" ) ) );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void findExistingCachedEntity(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> {
			var record = s.find( CachedRecord.class, 1L );
			assertNotNull( record );
			assertEquals( "hello", record.message );
		} );
		scope.inStatelessTransaction( s -> {
			var record = s.find( CachedRecord.class, 1L );
			assertNotNull( record );
			assertEquals( "hello", record.message );
		} );
	}

	@Test
	void findNonExistingCachedEntity(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> {
			var record = s.find( CachedRecord.class, 999L );
			assertNull( record );
		} );
	}

	@Entity(name = "CachedRecord")
	@Cacheable
	static class CachedRecord {
		@Id
		Long id;
		String message;

		CachedRecord(Long id, String message) {
			this.id = id;
			this.message = message;
		}

		CachedRecord() {
		}
	}
}
