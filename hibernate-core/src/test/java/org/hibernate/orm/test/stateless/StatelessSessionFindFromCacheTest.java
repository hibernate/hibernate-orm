package org.hibernate.orm.test.stateless;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = StatelessSessionFindFromCacheTest.CachedRecord.class)
public class StatelessSessionFindFromCacheTest {

	@Test
	void findExistingEntity(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> s.insert( new CachedRecord( 1L, "hello" ) ) );
		// first find loads from DB and populates the cache
		scope.inStatelessTransaction( s -> {
			CachedRecord record = s.find( CachedRecord.class, 1L );
			assertNotNull( record );
			assertEquals( "hello", record.message );
		} );
		// second find hits the second-level cache
		scope.inStatelessTransaction( s -> {
			CachedRecord record = s.find( CachedRecord.class, 1L );
			assertNotNull( record );
			assertEquals( "hello", record.message );
		} );
	}

	@Test
	void findNonExistingEntity(SessionFactoryScope scope) {
		scope.inStatelessTransaction( s -> {
			CachedRecord record = s.find( CachedRecord.class, 999L );
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
