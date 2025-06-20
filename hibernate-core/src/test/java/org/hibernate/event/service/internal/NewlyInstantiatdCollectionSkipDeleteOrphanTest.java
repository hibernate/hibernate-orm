/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Artem K.
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14178")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = {
				NewlyInstantiatdCollectionSkipDeleteOrphanTest.VersionedParent.class,
				NewlyInstantiatdCollectionSkipDeleteOrphanTest.UnversionedParent.class,
				NewlyInstantiatdCollectionSkipDeleteOrphanTest.Child.class,
				NewlyInstantiatdCollectionSkipDeleteOrphanTest.VersionedMappingUnversionedParent.class,
				NewlyInstantiatdCollectionSkipDeleteOrphanTest.VersionedMappingVersionedParent.class
		}
)
@SessionFactory
public class NewlyInstantiatdCollectionSkipDeleteOrphanTest {

	private UnversionedParent up;
	private VersionedParent vp;
	private Child c;
	private VersionedMappingUnversionedParent vmup;
	private VersionedMappingVersionedParent vmvp;

	@BeforeEach
	public void setup() {

		up = new UnversionedParent();

		vp = new VersionedParent();

		c = new Child();

		vmup = new VersionedMappingUnversionedParent();
		vmup.addChild( c );

		vmvp = new VersionedMappingVersionedParent();
		vmvp.addChild( c );

	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void VersionedMappingVersionedParentSaveUpdate(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.setHibernateFlushMode( FlushMode.MANUAL );

			Transaction trx = s.beginTransaction();
			try {
				// Associate the mapping to parent
				vp.addChild( vmvp );

				// Persist Child associated with versioned parent
				s.merge( c );
				assertNotEquals( Integer.valueOf( 0 ), c.getId() );

				// Persist VersionParent
				s.merge( vp );
				assertNotEquals( Integer.valueOf( 0 ), vp.getId() );

				// Persist versioned mapping now that parent id is generated
				s.merge( vmvp );
				assertNotNull( vmvp.getId() );
				assertNotEquals( Integer.valueOf( 0 ), vmvp.getId().getParentId() );
				assertNotEquals( Integer.valueOf( 0 ), vmvp.getId().getChildId() );

				s.flush();
				trx.commit();
			}
			catch (RuntimeException e) {
				// Transaction is rolled back so we do not want delete code in cleanup to execute.
				// Reset any possible ID assignments
				vp.setId( null );
				c.setId( null );

				if ( trx.isActive() ) {
					trx.rollback();
				}
				throw e;
			}
		} );
	}

	@Test
	public void VersionedMappingUnversionedParentSaveUpdate(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.setHibernateFlushMode( FlushMode.MANUAL );

			Transaction trx = s.beginTransaction();
			try {
				// Associate the mapping to parent
				up.addVersionedMappings( vmup );

				// Persist child associated with versioned mapping of unversioned parent
				s.merge( c );
				assertNotEquals( Integer.valueOf( 0 ), c.getId() );

				// Persist unversioned parent
				s.merge( up );
				assertNotEquals( Integer.valueOf( 0 ), up.getId() );

				// Persist versioned mapping
				s.merge( vmup );
				assertNotNull( vmup.getId() );
				assertNotEquals( Integer.valueOf( 0 ), vmup.getId().getParentId() );
				assertNotEquals( Integer.valueOf( 0 ), vmup.getId().getChildId() );

				s.flush();
				trx.commit();
			}
			catch (RuntimeException e) {
				// Transaction is rolled back so we do not want delete code in cleanup to execute.
				// Reset any possible ID assignments
				up.setId( null );
				c.setId( null );

				if ( trx.isActive() ) {
					trx.rollback();
				}
				throw e;
			}
		} );
	}

	@Entity(name = "Child")
	@Table(name = "Child")
	@DynamicUpdate
	public static class Child {
		private Integer id;
		private Long version;

		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "Id", nullable = false)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Version
		@Column(name = "Version", nullable = false)
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof Child ) ) {
				return false;
			}
			Child other = (Child) obj;
			return Objects.equals( getId(), other.getId() );
		}
	}

	@Embeddable
	public static class MappingId implements Serializable {
		private static final long serialVersionUID = -4896032953810358940L;

		private Integer parentId;
		private Integer childId;

		@Column(name = "ParentId", nullable = false)
		public Integer getParentId() {
			return parentId;
		}

		public void setParentId(Integer parentId) {
			this.parentId = parentId;
		}

		@Column(name = "ChildId", nullable = false)
		public Integer getChildId() {
			return childId;
		}

		public void setChildId(Integer childId) {
			this.childId = childId;
		}

		@Override
		public int hashCode() {
			return Objects.hash( getParentId(), getChildId() );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof MappingId ) ) {
				return false;
			}
			MappingId other = (MappingId) obj;
			return Objects.equals( getParentId(), other.getParentId() ) && Objects.equals(
					getChildId(),
					other.getChildId()
			);
		}

		@Override
		public String toString() {
			return "[" + getParentId() + " | " + getChildId() + "]";
		}
	}

	@Entity(name = "UnversionedParent")
	@Table(name = "UnversParent")
	@DynamicUpdate
	public static class UnversionedParent {
		private Integer id;
		private Set<VersionedMappingUnversionedParent> versionedMappings;

		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "Id", nullable = false)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			if ( !Objects.equals( id, getId() ) ) {
				this.id = id;

				getVersionedMappings().forEach( c -> {
					if ( c.getId() == null ) {
						c.setId( new MappingId() );
					}
					c.getId().setParentId( id );
				} );
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(mappedBy = "parent", cascade = {
				jakarta.persistence.CascadeType.DETACH,
				jakarta.persistence.CascadeType.MERGE,
				jakarta.persistence.CascadeType.REFRESH,
				jakarta.persistence.CascadeType.REMOVE
		}, orphanRemoval = true)
		@Cascade({
				CascadeType.REMOVE,
				org.hibernate.annotations.CascadeType.LOCK,
				org.hibernate.annotations.CascadeType.REPLICATE
		})
		protected Set<VersionedMappingUnversionedParent> getVersionedMappings() {
			if ( versionedMappings == null ) {
				versionedMappings = new HashSet<>();
			}
			return this.versionedMappings;
		}

		protected void setVersionedMappings(Set<VersionedMappingUnversionedParent> value) {
			if ( value == null && this.versionedMappings != null ) {
				this.versionedMappings.clear();
			}
			else {
				this.versionedMappings = value;
			}
		}

		@Transient
		public Collection<VersionedMappingUnversionedParent> getVersionedMappingsCollection() {
			return new ArrayList<>( getVersionedMappings() );
		}

		public void addVersionedMappings(VersionedMappingUnversionedParent addValue) {
			if ( addValue != null && !this.getVersionedMappings().contains( addValue ) ) {
				this.versionedMappings.add( addValue );
				addValue.addParent( this );
			}
		}

		public void removeVersionedMappings(VersionedMappingUnversionedParent removeValue) {
			if ( this.versionedMappings != null && this.versionedMappings.contains( removeValue ) ) {
				this.versionedMappings.remove( removeValue );
				removeValue.removeParent();
			}
		}

		@Override
		public int hashCode() {
			return 17;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof UnversionedParent ) ) {
				return false;
			}
			UnversionedParent other = (UnversionedParent) obj;
			return Objects.equals( getId(), other.getId() );
		}
	}

	@Entity(name = "VersionedParent")
	@Table(name = "VersParent")
	@DynamicUpdate
	public static class VersionedParent {
		private Integer id;
		private Long version;
		private Set<VersionedMappingVersionedParent> children;
		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "Id", nullable = false)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			if ( !Objects.equals( id, getId() ) ) {
				this.id = id;

				getChildren().forEach( c -> {
					if ( c.getId() == null ) {
						c.setId( new MappingId() );
					}
					c.getId().setParentId( id );
				} );
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Version
		@Column(name = "Version", nullable = false)
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		@OneToMany(mappedBy = "parent", cascade = {
				jakarta.persistence.CascadeType.DETACH,
				jakarta.persistence.CascadeType.MERGE,
				jakarta.persistence.CascadeType.REFRESH,
				jakarta.persistence.CascadeType.REMOVE
		}, orphanRemoval = true)
		@Cascade({
				CascadeType.REMOVE,
				org.hibernate.annotations.CascadeType.LOCK,
				org.hibernate.annotations.CascadeType.REPLICATE
		})
		protected Set<VersionedMappingVersionedParent> getChildren() {
			if ( children == null ) {
				children = new HashSet<>();
			}
			return this.children;
		}

		protected void setChildren(Set<VersionedMappingVersionedParent> value) {
			if ( value == null && this.children != null ) {
				this.children.clear();
			}
			else {
				this.children = value;
			}
		}

		@Transient
		public Collection<VersionedMappingVersionedParent> getChildrenCollection() {
			return new ArrayList<>( getChildren() );
		}

		public void addChild(VersionedMappingVersionedParent addValue) {
			if ( addValue != null && !this.getChildren().contains( addValue ) ) {
				this.children.add( addValue );
				addValue.addParent( this );
			}
		}

		public void removeChild(VersionedMappingVersionedParent removeValue) {
			if ( this.children != null && this.children.contains( removeValue ) ) {
				this.children.remove( removeValue );
				removeValue.removeParent();
			}
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof VersionedParent ) ) {
				return false;
			}
			VersionedParent other = (VersionedParent) obj;
			return Objects.equals( getId(), other.getId() );
		}
	}

	@Entity(name = "VersionedMappingUnversionedParent")
	@Table(name = "VersdMapUnversParent")
	@DynamicUpdate
	public static class VersionedMappingUnversionedParent {
		private MappingId id;
		private Child child;
		private Long version;
		private String name;

		@EmbeddedId
		public MappingId getId() {
			return this.id;
		}

		public void setId(MappingId id) {
			this.id = id;
		}

		@Version
		@Column(name = "Version", nullable = false)
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		protected UnversionedParent parent;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId("parentId")
		@JoinColumn(name = "ParentId", nullable = false)
		public UnversionedParent getParent() {
			return this.parent;
		}

		protected void setParent(UnversionedParent value) {
			this.parent = value;
		}

		public void addParent(UnversionedParent value) {
			UnversionedParent oldParent = getParent();
			if ( !Objects.equals( value, oldParent ) ) {
				if ( oldParent != null ) {
					setParent( null );
					oldParent.removeVersionedMappings( this );
				}

				if ( value != null ) {
					setParent( value );
					if ( getId() == null ) {
						setId( new MappingId() );
					}
					getId().setParentId( value.getId() );
					value.addVersionedMappings( this );
				}
			}
		}

		public void removeParent() {
			addParent( null );
		}

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId("childId")
		@JoinColumn(name = "ChildId", nullable = false)
		public Child getChild() {
			return child;
		}

		protected void setChild(Child child) {
			this.child = child;
		}

		public void addChild(Child value) {
			Child oldChild = getChild();
			if ( !Objects.equals( value, oldChild ) ) {
				if ( oldChild != null ) {
					setChild( null );
				}

				if ( value != null ) {
					setChild( value );
					if ( getId() == null ) {
						setId( new MappingId() );
					}
					getId().setChildId( value.getId() );
				}
			}
		}

		public void removeChild() {
			addChild( null );
		}

		@Override
		public int hashCode() {
			return 17;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof VersionedMappingUnversionedParent ) ) {
				return false;
			}
			VersionedMappingUnversionedParent other = (VersionedMappingUnversionedParent) obj;
			return Objects.equals( getId(), other.getId() );
		}
	}

	@Entity(name = "VersionedMappingVersionedParent")
	@Table(name = "VersMapVersParent")
	@DynamicUpdate
	public static class VersionedMappingVersionedParent {
		private MappingId id;
		private Child child;
		private Long version;
		private String name;

		@EmbeddedId
		public MappingId getId() {
			return this.id;
		}

		public void setId(MappingId id) {
			this.id = id;
		}

		@Version
		@Column(name = "Version", nullable = false)
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		protected VersionedParent parent;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId("parentId")
		@JoinColumn(name = "ParentId", nullable = false)
		public VersionedParent getParent() {
			return this.parent;
		}

		protected void setParent(VersionedParent value) {
			this.parent = value;
		}

		public void addParent(VersionedParent value) {
			VersionedParent oldParent = getParent();
			if ( !Objects.equals( value, oldParent ) ) {
				if ( oldParent != null ) {
					setParent( null );
					oldParent.removeChild( this );
				}

				if ( value != null ) {
					setParent( value );
					if ( getId() == null ) {
						setId( new MappingId() );
					}
					getId().setParentId( value.getId() );
					value.addChild( this );
				}
			}
		}

		public void removeParent() {
			addParent( null );
		}

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@MapsId("childId")
		@JoinColumn(name = "ChildId", nullable = false)
		public Child getChild() {
			return child;
		}

		protected void setChild(Child child) {
			this.child = child;
		}

		public void addChild(Child value) {
			Child oldChild = getChild();
			if ( !Objects.equals( value, oldChild ) ) {
				if ( oldChild != null ) {
					setChild( null );
				}

				if ( value != null ) {
					setChild( value );
					if ( getId() == null ) {
						setId( new MappingId() );
					}
					getId().setChildId( value.getId() );
				}
			}
		}

		public void removeChild() {
			addChild( null );
		}

		@Override
		public int hashCode() {
			return 17;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof VersionedMappingVersionedParent ) ) {
				return false;
			}
			VersionedMappingVersionedParent other = (VersionedMappingVersionedParent) obj;
			return Objects.equals( getId(), other.getId() );
		}
	}
}
