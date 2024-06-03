/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				MappedSuperclassWithEmbeddableTest.TestEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class MappedSuperclassWithEmbeddableTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			TestEntity testEntity = new TestEntity( "2", "test" );
			s.persist( testEntity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createQuery( "delete from TestEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			TestEntity testEntity = s.get( TestEntity.class, "2" );
			assertThat( testEntity ).isNotNull();
		} );
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Embedded
		private EmbeddedValue superField;

		public EmbeddedValue getSuperField() {
			return superField;
		}

		public void setSuperField(EmbeddedValue superField) {
			this.superField = superField;
		}
	}

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@Column(name = "super_field")
		private String superField;

		public EmbeddedValue() {
		}

		private EmbeddedValue(String superField) {
			this.superField = superField;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddedValue that = (EmbeddedValue) o;
			return superField.equals( that.superField );
		}

		@Override
		public int hashCode() {
			return Objects.hash( superField );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends BaseEntity {
		@Id
		private String id;
		private String name;

		public TestEntity() {
		}

		private TestEntity(String id, String name) {
			this.id = id;
			this.name = name;
			EmbeddedValue value = new EmbeddedValue( "SUPER " + name );
			setSuperField( value );
		}


		public String id() {
			return id;
		}

		public String name() {
			return name;
		}
	}

}
