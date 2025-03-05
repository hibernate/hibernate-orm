/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.version;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-15134")
@DomainModel(
		annotatedClasses = {
			VersionedEntityTest.FooEntity.class, VersionedEntityTest.BarEntity.class, VersionedEntityTest.BazEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class VersionedEntityTest {
	private final Long parentID = 1L;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final FooEntity entity = FooEntity.of( parentID, "foo" );
			em.persist( entity );
		} );
	}

	@Test
	public void testUpdateVersionedEntity(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final FooEntity entity = em.getReference( FooEntity.class, parentID );

			assertFalse( isInitialized( entity ) );
			assertTrue( Hibernate.isPropertyInitialized( entity, "id" ) );
			assertFalse( Hibernate.isPropertyInitialized( entity, "name" ) );
			assertFalse( Hibernate.isPropertyInitialized( entity, "version" ) );
			assertFalse( Hibernate.isPropertyInitialized( entity, "bars" ) );
			assertFalse( Hibernate.isPropertyInitialized( entity, "bazzes" ) );

			entity.setName( "bar" );
		});
	}

	@MappedSuperclass
	public static abstract class AbstractEntity<T extends Serializable> {

		public abstract T getId();

		public abstract void setId(T id);

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null || obj.getClass() != getClass()) return false;

			final AbstractEntity<?> other = (AbstractEntity<?>) obj;
			return getId() != null && getId().equals(other.getId());
		}
	}

	@Entity(name = "FooEntity")
	public static class FooEntity extends AbstractEntity<Long> {

		@Id
		private long id;
		@Version
		private int version;

		private String name;

		@OneToMany(mappedBy = "foo", cascade = CascadeType.ALL, targetEntity = BarEntity.class, orphanRemoval = true)
		public Set<BarEntity> bars = new HashSet<>();

		@OneToMany(mappedBy = "foo", cascade = CascadeType.ALL, targetEntity = BazEntity.class, orphanRemoval = true)
		public Set<BazEntity> bazzes = new HashSet<>();

		public static FooEntity of(long id, String name) {
			final FooEntity f = new FooEntity();
			f.id = id;
			f.name = name;
			return f;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<BarEntity> getBars() {
			return bars;
		}

		public void addBar(BarEntity bar) {
			bars.add(bar);
			bar.setFoo(this);
		}

		public void removeBar(BarEntity bar) {
			bars.remove(bar);
			bar.setFoo(null);
		}

		public Set<BazEntity> getBazzes() {
			return bazzes;
		}

		public void addBaz(BazEntity baz) {
			bazzes.add(baz);
			baz.setFoo(this);
		}

		public void removeBaz(BazEntity baz) {
			bazzes.remove(baz);
			baz.setFoo(null);
		}

		@Override
		public String toString() {
			return String.format("FooEntity: id=%d, version=%d, name=%s", id, version, name);
		}
	}

	@Entity(name = "BazEntity")
	public static class BazEntity extends AbstractEntity<Long> {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(foreignKey = @ForeignKey(name = "fk_baz_foo"), nullable = false)
		private FooEntity foo;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public FooEntity getFoo() {
			return foo;
		}

		public void setFoo(FooEntity foo) {
			this.foo = foo;
		}

		@Override
		public String toString() {
			return String.format("BazEntity: id=%d", id);
		}
	}

	@Entity(name = "BarEntity")
	public static class BarEntity extends AbstractEntity<Long> {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(foreignKey = @ForeignKey(name = "fk_bar_foo"), nullable = false)
		private FooEntity foo;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public FooEntity getFoo() {
			return foo;
		}

		public void setFoo(FooEntity foo) {
			this.foo = foo;
		}

		@Override
		public String toString() {
			return String.format("BarEntity: id=%d", id);
		}
	}
}
