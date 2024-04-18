/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceEmbeddedIdTest.PkEmbeddable.class,
		JoinedInheritanceEmbeddedIdTest.BasePk.class,
		JoinedInheritanceEmbeddedIdTest.BaseEntity.class,
		JoinedInheritanceEmbeddedIdTest.SubEntity.class,
} )
@SessionFactory
@ServiceRegistry( settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = Action.ACTION_CREATE_THEN_DROP ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17883" )
public class JoinedInheritanceEmbeddedIdTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		final BasePk basePk = new BasePk( 1, "lesson_1", "record_1" );
		scope.inSession( session -> {
			final BaseEntity baseEntity = session.find( BaseEntity.class, basePk );
			assertThat( baseEntity ).isNotNull().extracting( BaseEntity::getPrimaryKey ).isEqualTo( basePk );
		} );
		scope.inSession( session -> {
			final SubEntity subEntity = session.find( SubEntity.class, basePk );
			assertThat( subEntity ).isNotNull().extracting( SubEntity::getName ).isEqualTo( "sub_entity_1" );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SubEntity result = session.createQuery(
					"from SubEntity where primaryKey.siteCd = 1",
					SubEntity.class
			).getSingleResult();
			assertThat( result ).isNotNull().extracting( SubEntity::getName ).isEqualTo( "sub_entity_1" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasePk basePk = new BasePk( 1, "lesson_1", "record_1" );
			session.persist( new SubEntity( basePk, "sub_entity_1" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BaseEntity" ).executeUpdate() );
	}

	@MappedSuperclass
	static class PkEmbeddable implements Serializable {
		private Integer siteCd;

		public PkEmbeddable() {
		}

		public PkEmbeddable(Integer siteCd) {
			this.siteCd = siteCd;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final PkEmbeddable that = (PkEmbeddable) o;
			return Objects.equals( siteCd, that.siteCd );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( siteCd );
		}
	}

	@Embeddable
	static class BasePk extends PkEmbeddable {
		private String lessonCd;
		private String recordCd;

		public BasePk() {
		}

		public BasePk(Integer siteCd, String lessonCd, String recordCd) {
			super( siteCd );
			this.lessonCd = lessonCd;
			this.recordCd = recordCd;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}

			final BasePk basePk = (BasePk) o;
			return Objects.equals( lessonCd, basePk.lessonCd ) && Objects.equals(
					recordCd,
					basePk.recordCd
			);
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + Objects.hashCode( lessonCd );
			result = 31 * result + Objects.hashCode( recordCd );
			return result;
		}
	}

	@Entity( name = "BaseEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	static class BaseEntity {
		@EmbeddedId
		private BasePk primaryKey;

		public BaseEntity() {
		}

		public BaseEntity(BasePk primaryKey) {
			this.primaryKey = primaryKey;
		}

		public BasePk getPrimaryKey() {
			return primaryKey;
		}
	}

	@Entity( name = "SubEntity" )
	static class SubEntity extends BaseEntity {
		private String name;

		public SubEntity() {
		}

		public SubEntity(BasePk primaryKey, String name) {
			super( primaryKey );
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
