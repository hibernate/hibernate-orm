package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

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

	@Test
	void fieldWriteToAnotherEntityBeforeSuperCallTracksTargetEntityOnly() {
		var target = new OtherEntityFieldWriteEntity();
		var entity = new OtherEntityFieldWriteEntity( target, "target" );

		assertThat( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() )
				.doesNotContain( "title" );
		assertThat( ( (SelfDirtinessTracker) target ).$$_hibernate_getDirtyAttributes() )
				.contains( "title" );
	}

	@Test
	void noOpFieldAssignmentsBeforeSuperCallAreNotDirty() {
		var entity = new NoOpDefaultFlexConstructorEntity( null, 0 );

		assertThat( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() )
				.doesNotContain( "title", "count" );
	}

	@Test
	void mappedCollectionAssignmentBeforeSuperCallIsNotTrackedAsBasicDirtyField() {
		var entity = new MappedCollectionFlexConstructorEntity( List.of() );

		assertThat( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() )
				.doesNotContain( "items" );
	}

	@Test
	void identifierOnlyAssignmentBeforeSuperCallIsNotDirty() {
		var entity = new AssignedIdFlexConstructorEntity( 42L );
		assertThat( entity.id ).isEqualTo( 42L );
		assertThat( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() )
				.doesNotContain( "id" );
	}

	@Test
	void embeddableAssignmentBeforeSuperCallPreservesValue() {
		assertThat( new FlexConstructorAddress( "Main St" ).street ).isEqualTo( "Main St" );
	}

	@Test
	void mappedSuperclassAssignmentBeforeSuperCallPreservesValue() {
		assertThat( new InheritedFlexConstructorEntity( "inherited" ).getTitle() ).isEqualTo( "inherited" );
	}

	@Test
	void unrelatedObjectFieldWriteBeforeSuperCallDoesNotDirtyEntity() {
		var target = new PlainObject();
		var entity = new UnrelatedFieldWriteEntity( target );
		assertThat( target.title ).isEqualTo( "changed" );
		assertThat( ( (SelfDirtinessTracker) entity ).$$_hibernate_getDirtyAttributes() )
				.doesNotContain( "title" );
	}

	@Entity(name = "AssignedIdFlexConstructorEntity")
	public static class AssignedIdFlexConstructorEntity {
		@Id
		private Long id;

		protected AssignedIdFlexConstructorEntity() {
		}

		public AssignedIdFlexConstructorEntity(Long id) {
			this.id = id;
			super();
		}
	}

	@Embeddable
	public static class FlexConstructorAddress {
		private String street;

		protected FlexConstructorAddress() {
		}

		public FlexConstructorAddress(String street) {
			this.street = street;
			super();
		}
	}

	@MappedSuperclass
	public static class FlexConstructorMappedSuperclass {
		private String title;

		protected FlexConstructorMappedSuperclass() {
		}

		public FlexConstructorMappedSuperclass(String title) {
			this.title = title;
			super();
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "InheritedFlexConstructorEntity")
	public static class InheritedFlexConstructorEntity extends FlexConstructorMappedSuperclass {
		@Id
		private Long id;

		protected InheritedFlexConstructorEntity() {
		}

		public InheritedFlexConstructorEntity(String title) {
			super( title );
		}
	}

	public static class PlainObject {
		public String title;
	}

	@Entity(name = "UnrelatedFieldWriteEntity")
	public static class UnrelatedFieldWriteEntity {
		@Id
		private Long id;

		private String title;

		protected UnrelatedFieldWriteEntity() {
		}

		public UnrelatedFieldWriteEntity(PlainObject other) {
			other.title = "changed";
			super();
		}
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

	@Entity(name = "MappedCollectionFlexConstructorEntity")
	public static class MappedCollectionFlexConstructorEntity {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "parent")
		private List<MappedCollectionFlexConstructorItem> items;

		protected MappedCollectionFlexConstructorEntity() {
		}

		public MappedCollectionFlexConstructorEntity(List<MappedCollectionFlexConstructorItem> items) {
			this.items = items;
			super();
		}

		public List<MappedCollectionFlexConstructorItem> getItems() {
			return items;
		}
	}

	@Entity(name = "MappedCollectionFlexConstructorItem")
	public static class MappedCollectionFlexConstructorItem {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private MappedCollectionFlexConstructorEntity parent;

		protected MappedCollectionFlexConstructorItem() {
		}

		public MappedCollectionFlexConstructorItem(MappedCollectionFlexConstructorEntity parent) {
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public MappedCollectionFlexConstructorEntity getParent() {
			return parent;
		}
	}

	@Entity(name = "NoOpDefaultFlexConstructorEntity")
	public static class NoOpDefaultFlexConstructorEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private int count;

		protected NoOpDefaultFlexConstructorEntity() {
		}

		public NoOpDefaultFlexConstructorEntity(String title, int count) {
			this.title = title;
			this.count = count;
			super();
		}

		public String getTitle() {
			return title;
		}

		public int getCount() {
			return count;
		}
	}

	@Entity(name = "OtherEntityFieldWriteEntity")
	public static class OtherEntityFieldWriteEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public OtherEntityFieldWriteEntity() {
		}

		public OtherEntityFieldWriteEntity(OtherEntityFieldWriteEntity other, String title) {
			other.title = title;
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
