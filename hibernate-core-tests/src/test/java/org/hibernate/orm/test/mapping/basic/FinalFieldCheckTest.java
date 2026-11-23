package org.hibernate.orm.test.mapping.basic;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.annotations.Instantiator;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Jira("https://hibernate.atlassian.net/browse/HHH-20479")
@MessageKeyInspection(
		messageKey = "HHH006597",
		logger = @Logger(loggerName = CoreMessageLogger.NAME)
)
public class FinalFieldCheckTest {

	@Test
	@DomainModel(annotatedClasses = SimpleEntityWithFinalField.class)
	@SessionFactory
	void testFinalFieldWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isTrue();
		assertThat( watcher.getTriggeredMessages() ).hasSize( 1 );
		assertThat( watcher.getTriggeredMessages().get( 0 ) ).contains( "immutable" );
		assertThat( watcher.getTriggeredMessages().get( 0 ) ).contains( "entity" );
	}

	@Test
	@DomainModel(annotatedClasses = SimpleEntityWithFinalField.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(
			name = MappingSettings.FINAL_PERSISTENT_FIELDS,
			value = "ignore"
	))
	void testFinalFieldWarningSuppressed(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isFalse();
	}

	@Test
	@DomainModel(annotatedClasses = SimpleEntityWithFinalField.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(
			name = MappingSettings.FINAL_PERSISTENT_FIELDS,
			value = "error"
	))
	void testFinalFieldError(SessionFactoryScope scope) {
		assertThatThrownBy( scope::getSessionFactory )
				.isInstanceOf( Exception.class )
				.hasMessageContaining( "immutable" );
	}

	@Test
	@DomainModel(annotatedClasses = EntityWithFinalEmbeddable.class)
	@SessionFactory
	void testFinalEmbeddableFieldWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isTrue();
		assertThat( watcher.getTriggeredMessages() ).anySatisfy( message -> {
			assertThat( message ).contains( "embeddable" );
			assertThat( message ).contains( "part1" );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = EntityWithRecordEmbeddedId.class)
	@SessionFactory
	void testRecordEmbeddedIdNoWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isFalse();
	}

	@Test
	@DomainModel(annotatedClasses = EntityWithFinalIdClass.class)
	@SessionFactory
	void testFinalIdClassFieldWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isTrue();
		assertThat( watcher.getTriggeredMessages() ).anySatisfy( message -> {
			assertThat( message ).contains( "id class" );
			assertThat( message ).contains( "key1" );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = EntityWithRecordEmbedded.class)
	@SessionFactory
	void testRecordEmbeddedNoWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isFalse();
	}

	@Test
	@DomainModel(annotatedClasses = EntityWithInstantiatorEmbeddable.class)
	@SessionFactory
	void testInstantiatorEmbeddableNoWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isFalse();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20542")
	@DomainModel(annotatedClasses = EntityWithInstantiatorIdClass.class)
	@SessionFactory
	void testInstantiatorIdClassNoWarning(MessageKeyWatcher watcher, SessionFactoryScope scope) {
		scope.getSessionFactory();
		assertThat( watcher.wasTriggered() ).isFalse();
	}

	@Entity(name = "SimpleEntityWithFinalField")
	public static class SimpleEntityWithFinalField {
		@Id
		@GeneratedValue
		private Long id;

		private final String immutable;

		protected SimpleEntityWithFinalField() {
			this.immutable = null;
		}

		public SimpleEntityWithFinalField(String immutable) {
			this.immutable = immutable;
		}

		public Long getId() {
			return id;
		}

		public String getImmutable() {
			return immutable;
		}
	}

	@Entity(name = "EntityWithFinalEmbeddable")
	public static class EntityWithFinalEmbeddable {
		@EmbeddedId
		private EmbeddableWithFinalFields id;

		protected EntityWithFinalEmbeddable() {
		}
	}

	@Embeddable
	public static class EmbeddableWithFinalFields implements Serializable {
		private final Long part1;
		private final Long part2;

		protected EmbeddableWithFinalFields() {
			this.part1 = null;
			this.part2 = null;
		}

		public EmbeddableWithFinalFields(Long part1, Long part2) {
			this.part1 = part1;
			this.part2 = part2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddableWithFinalFields that = (EmbeddableWithFinalFields) o;
			return Objects.equals( part1, that.part1 ) && Objects.equals( part2, that.part2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( part1, part2 );
		}
	}

	@Entity(name = "EntityWithRecordEmbeddedId")
	public static class EntityWithRecordEmbeddedId {
		@EmbeddedId
		private RecordId id;

		private String name;

		protected EntityWithRecordEmbeddedId() {
		}

		public EntityWithRecordEmbeddedId(RecordId id, String name) {
			this.id = id;
			this.name = name;
		}

		public RecordId getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public record RecordId(Long part1, Long part2) implements Serializable {}

	public static class FinalFieldIdClass implements Serializable {
		private final Long key1;
		private final String key2;

		public FinalFieldIdClass() {
			this.key1 = null;
			this.key2 = null;
		}

		public FinalFieldIdClass(Long key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
		}

		public Long getKey1() {
			return key1;
		}

		public String getKey2() {
			return key2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			FinalFieldIdClass that = (FinalFieldIdClass) o;
			return Objects.equals( key1, that.key1 ) && Objects.equals( key2, that.key2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( key1, key2 );
		}
	}

	@Entity(name = "EntityWithRecordEmbedded")
	public static class EntityWithRecordEmbedded {
		@Id
		@GeneratedValue
		private Long id;

		@Embedded
		private RecordComponent component;

		protected EntityWithRecordEmbedded() {
		}

		public EntityWithRecordEmbedded(RecordComponent component) {
			this.component = component;
		}
	}

	@Embeddable
	public record RecordComponent(String value1, String value2) {}

	@Entity(name = "EntityWithInstantiatorEmbeddable")
	public static class EntityWithInstantiatorEmbeddable {
		@Id
		@GeneratedValue
		private Long id;

		@Embedded
		private InstantiatorEmbeddable component;

		protected EntityWithInstantiatorEmbeddable() {
		}
	}

	@Embeddable
	public static class InstantiatorEmbeddable {
		private final String value1;
		private final String value2;

		@Instantiator({ "value1", "value2" })
		public InstantiatorEmbeddable(String value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		public String getValue1() {
			return value1;
		}

		public String getValue2() {
			return value2;
		}
	}

	@Entity(name = "EntityWithFinalIdClass")
	@IdClass(FinalFieldIdClass.class)
	public static class EntityWithFinalIdClass {
		@Id
		private Long key1;

		@Id
		private String key2;

		private String data;

		protected EntityWithFinalIdClass() {
		}

		public EntityWithFinalIdClass(Long key1, String key2, String data) {
			this.key1 = key1;
			this.key2 = key2;
			this.data = data;
		}

		public Long getKey1() {
			return key1;
		}

		public String getKey2() {
			return key2;
		}

		public String getData() {
			return data;
		}
	}

	public static class InstantiatorIdClass implements Serializable {
		private final Long key1;
		private final String key2;

		public InstantiatorIdClass() {
			this.key1 = null;
			this.key2 = null;
		}

		@Instantiator({ "key1", "key2" })
		public InstantiatorIdClass(Long key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
		}

		public Long getKey1() {
			return key1;
		}

		public String getKey2() {
			return key2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			InstantiatorIdClass that = (InstantiatorIdClass) o;
			return Objects.equals( key1, that.key1 ) && Objects.equals( key2, that.key2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( key1, key2 );
		}
	}

	@Entity(name = "EntityWithInstantiatorIdClass")
	@IdClass(InstantiatorIdClass.class)
	public static class EntityWithInstantiatorIdClass {
		@Id
		private Long key1;

		@Id
		private String key2;

		private String data;

		protected EntityWithInstantiatorIdClass() {
		}

		public EntityWithInstantiatorIdClass(Long key1, String key2, String data) {
			this.key1 = key1;
			this.key2 = key2;
			this.data = data;
		}

		public Long getKey1() {
			return key1;
		}

		public String getKey2() {
			return key2;
		}

		public String getData() {
			return data;
		}
	}
}
