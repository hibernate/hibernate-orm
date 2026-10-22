package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME;

/**
 * Verifies that bytecode enhancement does not break entities
 * using Java 25+ flexible constructors (JEP 513),
 * where field assignments appear before the {@code super()} call.
 */
@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class FlexibleConstructorEnhancementTest {

	@Test
	void entityWithFieldAssignmentBeforeSuperCallTracksDirtyFields() {
		var entity = new FlexConstructorEntity( "test" );
		assertThat( entity ).isInstanceOf( SelfDirtinessTracker.class );

		var tracker = (SelfDirtinessTracker) entity;
		assertThat( tracker.$$_hibernate_hasDirtyAttributes() ).isTrue();
		assertThat( tracker.$$_hibernate_getDirtyAttributes() ).contains( "title" );
	}

	@Test
	void conditionalFieldWriteBeforeSuperCallOnlyTracksDirtyWhenWritten() {
		var withTitle = new ConditionalFlexConstructorEntity( "hello" );
		var trackerWithTitle = (SelfDirtinessTracker) withTitle;
		assertThat( trackerWithTitle.$$_hibernate_getDirtyAttributes() ).contains( "title" );

		var withoutTitle = new ConditionalFlexConstructorEntity( null );
		var trackerWithoutTitle = (SelfDirtinessTracker) withoutTitle;
		assertThat( trackerWithoutTitle.$$_hibernate_getDirtyAttributes() ).doesNotContain( "title" );
	}

	@Test
	void compositeFieldWriteBeforeSuperCallSetsUpOwnerTracking() {
		var address = new CompositeAddress( "123 Main St" );
		var entity = new CompositeFlexConstructorEntity( address );

		var tracker = (SelfDirtinessTracker) entity;
		assertThat( tracker.$$_hibernate_getDirtyAttributes() ).contains( "address" );

		assertThat( address )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME )
				.isInstanceOf( CompositeOwnerTracker.class );
	}

	@Embeddable
	public static class CompositeAddress {

		private String street;

		public CompositeAddress() {
		}

		public CompositeAddress(String street) {
			this.street = street;
		}

		public String getStreet() {
			return street;
		}
	}

	@Entity(name = "CompositeFlexConstructorEntity")
	public static class CompositeFlexConstructorEntity {

		@Id
		@GeneratedValue
		private Long id;

		@Embedded
		private CompositeAddress address;

		protected CompositeFlexConstructorEntity() {
		}

		public CompositeFlexConstructorEntity(CompositeAddress address) {
			this.address = address;
			super();
		}

		public Long getId() {
			return id;
		}

		public CompositeAddress getAddress() {
			return address;
		}
	}

	@Entity(name = "ConditionalFlexConstructorEntity")
	public static class ConditionalFlexConstructorEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		protected ConditionalFlexConstructorEntity() {
		}

		public ConditionalFlexConstructorEntity(String title) {
			if ( title != null ) {
				this.title = title;
			}
			super();
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "FlexConstructorEntity")
	public static class FlexConstructorEntity {

		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private String title;

		protected FlexConstructorEntity() {
		}

		public FlexConstructorEntity(String title) {
			Objects.requireNonNull( title );
			if ( title.isBlank() || title.length() > 255 ) {
				throw new IllegalArgumentException( "Title must contain between 1 and 255 characters." );
			}
			this.title = title;
			super();
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}
}
