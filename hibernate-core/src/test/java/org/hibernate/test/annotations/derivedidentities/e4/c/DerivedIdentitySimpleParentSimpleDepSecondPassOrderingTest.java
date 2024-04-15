/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.c;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.test.util.SchemaUtil.getColumnNames;

/**
 * Test that when an entity with derived identity is referenced from a third entity,
 * associations on that third entity are bound in a second pass *after* the derived identity is bound.
 *
 * This test used to fail on bootstrap with the following error:
 *
 * org.hibernate.MappingException: Foreign key (FK2m2b1kaetxfvcsaih4raaocn8:ref_mto_derived [])) must have same number of columns as the referenced primary key (mto_derived [idsource_id])
 */
@TestForIssue(jiraKey = "HHH-14467")
public class DerivedIdentitySimpleParentSimpleDepSecondPassOrderingTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testOneToOne() {
		assertThat( getColumnNames( "oto_derived", metadata() ) )
				.contains( "idsource_id" )
				.doesNotContain( "id", "idSource", "idsource" );

		inTransaction( s -> {
			EntityWithSimpleId simple = new EntityWithSimpleId( 1 );
			s.persist( simple );
			EntityWithOneToOneDerivedId derived = new EntityWithOneToOneDerivedId( simple );
			s.persist( derived );
		} );

		inTransaction( s -> {
			EntityWithOneToOneDerivedId derived = s.get( EntityWithOneToOneDerivedId.class, 1 );
			assertThat( derived.getIdsource().getId() ).isEqualTo( 1 );
			derived.setData( "something" );
		} );

		inTransaction( s -> {
			EntityWithOneToOneDerivedId derived = s.get( EntityWithOneToOneDerivedId.class, 1 );
			assertThat( derived.getData() ).isNotNull();
		} );

		inTransaction( s -> {
			EntityWithOneToOneDerivedId derived = s.get( EntityWithOneToOneDerivedId.class, 1 );
			EntityReferencingEntityWithOneToOneDerivedId referencing =
					new EntityReferencingEntityWithOneToOneDerivedId( 1, derived );
			s.persist( referencing );
		} );

		inTransaction( s -> {
			EntityReferencingEntityWithOneToOneDerivedId referencing =
					s.get( EntityReferencingEntityWithOneToOneDerivedId.class, 1 );
			assertThat( referencing.getRef().getIdsource().getId() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testManyToOne() {
		assertThat( getColumnNames( "mto_derived", metadata() ) )
				.contains( "idsource_id" )
				.doesNotContain( "id", "idSource", "idsource" );

		inTransaction( s -> {
			EntityWithSimpleId simple = new EntityWithSimpleId( 2 );
			s.persist( simple );
			EntityWithManyToOneDerivedId derived = new EntityWithManyToOneDerivedId( simple );
			s.persist( derived );
		} );

		inTransaction( s -> {
			EntityWithManyToOneDerivedId derived = s.get( EntityWithManyToOneDerivedId.class, 2 );
			assertThat( derived.getIdsource().getId() ).isEqualTo( 2 );
			derived.setData( "something" );
		} );

		inTransaction( s -> {
			EntityWithManyToOneDerivedId derived = s.get( EntityWithManyToOneDerivedId.class, 2 );
			assertThat( derived.getData() ).isNotNull();
		} );

		inTransaction( s -> {
			EntityWithManyToOneDerivedId derived = s.get( EntityWithManyToOneDerivedId.class, 2 );
			EntityReferencingEntityWithManyToOneDerivedId referencing =
					new EntityReferencingEntityWithManyToOneDerivedId( 2, derived );
			s.persist( referencing );
		} );

		inTransaction( s -> {
			EntityReferencingEntityWithManyToOneDerivedId referencing =
					s.get( EntityReferencingEntityWithManyToOneDerivedId.class, 2 );
			assertThat( referencing.getRef().getIdsource().getId() ).isEqualTo( 2 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				// These two must be mentioned first in order to trigger the bug.
				// In a real application this would happen randomly, depending on how JARs are scanned.
				EntityReferencingEntityWithOneToOneDerivedId.class,
				EntityReferencingEntityWithManyToOneDerivedId.class,

				EntityWithSimpleId.class,
				EntityWithOneToOneDerivedId.class,
				EntityWithManyToOneDerivedId.class
		};
	}

	@Entity(name = "simple")
	public static class EntityWithSimpleId {
		@Id
		private Integer id;

		public EntityWithSimpleId() {
		}

		public EntityWithSimpleId(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "oto_derived")
	public static class EntityWithOneToOneDerivedId implements Serializable {
		@Id
		@OneToOne
		private EntityWithSimpleId idsource;

		private String data;

		public EntityWithOneToOneDerivedId() {
		}

		public EntityWithOneToOneDerivedId(EntityWithSimpleId idsource) {
			this.idsource = idsource;
		}

		public EntityWithSimpleId getIdsource() {
			return idsource;
		}

		public void setIdsource(EntityWithSimpleId idsource) {
			this.idsource = idsource;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name = "ref_oto_derived")
	public static class EntityReferencingEntityWithOneToOneDerivedId implements Serializable {
		@Id
		private Integer id;

		@ManyToOne
		private EntityWithOneToOneDerivedId ref;

		public EntityReferencingEntityWithOneToOneDerivedId() {
		}

		public EntityReferencingEntityWithOneToOneDerivedId(Integer id, EntityWithOneToOneDerivedId ref) {
			this.id = id;
			this.ref = ref;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityWithOneToOneDerivedId getRef() {
			return ref;
		}

		public void setRef(EntityWithOneToOneDerivedId ref) {
			this.ref = ref;
		}
	}

	@Entity(name = "mto_derived")
	public static class EntityWithManyToOneDerivedId implements Serializable {
		@Id
		@ManyToOne
		private EntityWithSimpleId idsource;

		private String data;

		public EntityWithManyToOneDerivedId() {
		}

		public EntityWithManyToOneDerivedId(EntityWithSimpleId idsource) {
			this.idsource = idsource;
		}

		public EntityWithSimpleId getIdsource() {
			return idsource;
		}

		public void setIdsource(EntityWithSimpleId idsource) {
			this.idsource = idsource;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name = "ref_mto_derived")
	public static class EntityReferencingEntityWithManyToOneDerivedId implements Serializable {
		@Id
		private Integer id;

		@ManyToOne
		private EntityWithManyToOneDerivedId ref;

		public EntityReferencingEntityWithManyToOneDerivedId() {
		}

		public EntityReferencingEntityWithManyToOneDerivedId(Integer id, EntityWithManyToOneDerivedId ref) {
			this.id = id;
			this.ref = ref;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityWithManyToOneDerivedId getRef() {
			return ref;
		}

		public void setRef(EntityWithManyToOneDerivedId ref) {
			this.ref = ref;
		}
	}

}
