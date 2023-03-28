/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.batch;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.ORDER_INSERTS;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16155" )
@JiraKey( "HHH-16319" )
@ServiceRegistry( settings = {
		@Setting( name = STATEMENT_BATCH_SIZE, value = "2" ),
		@Setting( name = ORDER_INSERTS, value = "true" ),
} )
@DomainModel( annotatedClasses = {
		GeneratedValuesWithBatchingTests.User.class,
		GeneratedValuesWithBatchingTests.Group.class
} )
@SessionFactory( useCollectingStatementInspector = true )
public class GeneratedValuesWithBatchingTests {

	@Test
	public void testBatched(SessionFactoryScope scope) {
		final User user1 = new User( 1, "fffs" );
		final User user2 = new User( 2, "baccab" );
		final User user3 = new User( 3, "xhgfd" );

		final Group group1 = new Group( 1, "admin", user1 );
		final Group group2 = new Group( 2, "owner", user3 );

		final SQLStatementInspector statementCollector = scope.getCollectingStatementInspector();
		statementCollector.clear();

		scope.inTransaction( (session) -> {
			session.persist( group1 );
			session.persist( group2 );
			session.persist( user1 );
			session.persist( user2 );
			session.persist( user3 );
		} );

		assertThat( statementCollector.getSqlQueries() ).hasSize( 10 );
		assertThat( statementCollector.getSqlQueries().get( 0 ) ).startsWith( "insert into t_users " );
		assertThat( statementCollector.getSqlQueries().get( 2 ) ).startsWith( "insert into t_users " );
		assertThat( statementCollector.getSqlQueries().get( 4 ) ).startsWith( "insert into t_users " );
		assertThat( statementCollector.getSqlQueries().get( 6 ) ).startsWith( "insert into t_groups " );
		assertThat( statementCollector.getSqlQueries().get( 8 ) ).startsWith( "insert into t_groups " );
	}

	@Entity( name = "User" )
	@Table( name = "t_users" )
	public static class User {
		@Id
		private Integer id;
		private String username;
		@DatabaseCreationTimestamp
		private Instant created;
		@DatabaseUpdateTimestamp
		private Instant updated;

		public User() {
		}

		public User(Integer id, String username) {
			this.id = id;
			this.username = username;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Instant getCreated() {
			return created;
		}

		public void setCreated(Instant created) {
			this.created = created;
		}

		public Instant getUpdated() {
			return updated;
		}

		public void setUpdated(Instant updated) {
			this.updated = updated;
		}
	}

	@Entity( name = "Group" )
	@Table( name = "t_groups" )
	public static class Group {
		@Id
		private Integer id;
		private String name;
		@ManyToOne( cascade = CascadeType.PERSIST )
		private User owner;

		@DatabaseCreationTimestamp
		private Instant created;
		@DatabaseUpdateTimestamp
		private Instant updated;

		public Group() {
		}

		public Group(Integer id, String name, User owner) {
			this.id = id;
			this.name = name;
			this.owner = owner;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Instant getCreated() {
			return created;
		}

		public void setCreated(Instant created) {
			this.created = created;
		}

		public Instant getUpdated() {
			return updated;
		}

		public void setUpdated(Instant updated) {
			this.updated = updated;
		}
	}

	@ValueGenerationType(generatedBy = DatabaseCreationTimestampGeneration.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DatabaseCreationTimestamp {
	}

	@ValueGenerationType(generatedBy = DatabaseUpdateTimestampGeneration.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DatabaseUpdateTimestamp {
	}

	public static abstract class AbstractDatabaseTimestampGenerator<A extends Annotation> implements AnnotationValueGeneration<A> {
		public boolean referenceColumnInSql() {
			return true;
		}

		public String getDatabaseGeneratedReferencedColumnValue() {
			return "current_timestamp";
		}

		@Override
		public void initialize(A annotation, Class<?> propertyType) {
			// nothing to do
		}

		public ValueGenerator<?> getValueGenerator() {
			// no in-memory generation
			return null;
		}
	}

	public static class DatabaseCreationTimestampGeneration extends AbstractDatabaseTimestampGenerator<DatabaseCreationTimestamp> {
		public GenerationTiming getGenerationTiming() {
			// its creation...
			return GenerationTiming.INSERT;
		}
	}

	public static class DatabaseUpdateTimestampGeneration extends AbstractDatabaseTimestampGenerator<DatabaseCreationTimestamp> {
		public GenerationTiming getGenerationTiming() {
			// its creation...
			return GenerationTiming.ALWAYS;
		}
	}
}
