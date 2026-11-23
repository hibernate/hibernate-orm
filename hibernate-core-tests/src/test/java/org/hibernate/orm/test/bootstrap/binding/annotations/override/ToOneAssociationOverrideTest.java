package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServiceRegistry
@JiraKey("HHH-13248")
class ToOneAssociationOverrideTest {
	@ParameterizedTest
	@ValueSource(classes = { BareOverride.class, NamedForeignKey.class, EmbeddedOwner.class })
	void missingJoinColumns(Class<?> owner, ServiceRegistryScope scope) {
		var sources = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Event.class )
				.addAnnotatedClass( owner );
		var failure = assertThrows( AnnotationException.class, sources::buildMetadata );
		String property = owner == EmbeddedOwner.class ? ".details.event" : ".event";
		assertThat( failure ).hasMessageContaining( owner.getName() + property ).hasMessageContaining( "joinColumns" );
		if ( owner != BareOverride.class ) {
			assertThat( failure ).hasMessageContaining( "fk_event" );
		}
	}

	@Test
	void inheritedAssociationOnImplicitJoinTable(ServiceRegistryScope scope) {
		var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Event.class )
				.addAnnotatedClass( TableOverride.class )
				.buildMetadata();
		var owner = metadata.getEntityBinding( TableOverride.class.getName() );
		var event = owner.getProperty( "event" );
		assertThat( owner.getJoins() ).singleElement().satisfies( join -> {
			assertThat( join.getProperties() ).contains( event );
			assertThat( join.getTable() ).isSameAs( event.getValue().getTable() );
		} );
		assertThat( event.getColumns() ).extracting( Column::getName )
				.containsExactly( "target_id" );
		assertThat( owner.getDeclaredProperties() ).doesNotContain( event );
	}

	@Test
	void overridingAnExistingJoinTable(ServiceRegistryScope scope) {
		var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Event.class )
				.addAnnotatedClass( DefaultTableOverride.class )
				.addAnnotatedClass( ColumnOverride.class )
				.buildMetadata();
		var tableOwner = metadata.getEntityBinding( DefaultTableOverride.class.getName() );
		assertThat( tableOwner.getJoins() ).singleElement();
		var columnOwner = metadata.getEntityBinding( ColumnOverride.class.getName() );
		assertThat( columnOwner.getJoins() ).isEmpty();
		assertThat( columnOwner.getProperty( "event" ).getColumns() ).extracting( Column::getName )
				.containsExactly( "new_event_id" );
	}

	@Test
	void joinTableOverrideWithPropertyAccess(ServiceRegistryScope scope) {
		var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Event.class )
				.addAnnotatedClass( PropertyAccessOverride.class )
				.buildMetadata();
		var owner = metadata.getEntityBinding( PropertyAccessOverride.class.getName() );
		assertThat( owner.getJoins() ).singleElement()
				.satisfies( join -> assertThat( join.getTable().getName() ).isEqualTo( "getter_events" ) );
		assertThat( owner.getProperty( "event" ).getValue().getTable().getName() ).isEqualTo( "getter_events" );
	}

	@Entity
	static class Event {
		@Id long id;
	}

	@MappedSuperclass
	static class Base {
		@Id long id;
		@OneToOne @JoinColumn(name = "event_id") Event event;
	}

	@Entity
	@AssociationOverride(name = "event")
	static class BareOverride extends Base {}

	@Entity
	@AssociationOverride(name = "event", foreignKey = @ForeignKey(name = "fk_event"))
	static class NamedForeignKey extends Base {}

	@Entity
	@AssociationOverride(name = "event", joinTable = @JoinTable(inverseJoinColumns = @JoinColumn(name = "target_id")))
	static class TableOverride extends Base {}

	@MappedSuperclass
	static class TableBase {
		@Id long id;
		@OneToOne @JoinTable(name = "old_events") Event event;
	}

	@Entity
	@AssociationOverride(name = "event", joinTable = @JoinTable)
	static class DefaultTableOverride extends TableBase {}

	@Entity
	@AssociationOverride(name = "event", joinColumns = @JoinColumn(name = "new_event_id"))
	static class ColumnOverride extends TableBase {}

	@Embeddable
	static class Details {
		@OneToOne @JoinColumn(name = "event_id") Event event;
	}

	@Entity
	static class EmbeddedOwner {
		@Id long id;
		@Embedded @AssociationOverride(name = "event", foreignKey = @ForeignKey(name = "fk_event")) Details details;
	}

	@MappedSuperclass
	@Access(AccessType.PROPERTY)
	static class PropertyAccessBase {
		private long id;
		private Event event;

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		@OneToOne
		public Event getEvent() {
			return event;
		}

		public void setEvent(Event event) {
			this.event = event;
		}
	}

	@Entity
	@AssociationOverride(name = "event", joinTable = @JoinTable(name = "getter_events"))
	static class PropertyAccessOverride extends PropertyAccessBase {}
}
