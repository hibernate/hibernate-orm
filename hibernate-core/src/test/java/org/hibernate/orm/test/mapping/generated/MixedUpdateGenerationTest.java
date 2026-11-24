package org.hibernate.orm.test.mapping.generated;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.SourceType.VM;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = { MixedUpdateGenerationTest.JavaFirst.class, MixedUpdateGenerationTest.SqlFirst.class })
@SessionFactory
class MixedUpdateGenerationTest {
	@Test
	void bothGeneratorsRunRegardlessOfPropertyOrder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new JavaFirst() );
			session.persist( new SqlFirst() );
		} );
		scope.inTransaction( session -> {
			final var javaFirst = session.find( JavaFirst.class, 1 );
			final var sqlFirst = session.find( SqlFirst.class, 1 );
			javaFirst.name = sqlFirst.name = "changed";
			session.flush();
			assertValues( javaFirst, sqlFirst );
		} );
		scope.inSession( session -> assertValues(
				session.find( JavaFirst.class, 1 ), session.find( SqlFirst.class, 1 ) ) );
	}

	private static void assertValues(JavaFirst javaFirst, SqlFirst sqlFirst) {
		assertNotNull( javaFirst.createdInJava );
		assertEquals( "updated", javaFirst.generatedInSql );
		assertNotNull( sqlFirst.generatedInJava );
		assertEquals( "updated", sqlFirst.createdInSql );
	}

	@MappedSuperclass
	static class Item {
		@Id int id = 1;
		String name;
	}

	@Entity(name = "JavaFirst")
	static class JavaFirst extends Item {
		@CurrentTimestamp(event = UPDATE, source = VM) Instant createdInJava;
		@Generated(event = UPDATE, sql = "'updated'") String generatedInSql;
	}

	@Entity(name = "SqlFirst")
	static class SqlFirst extends Item {
		@Generated(event = UPDATE, sql = "'updated'") String createdInSql;
		@CurrentTimestamp(event = UPDATE, source = VM) Instant generatedInJava;
	}
}
