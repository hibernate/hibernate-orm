package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.engine.spi.SelfDirtinessTracker;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-18402")
@DomainModel(annotatedClasses = DirtyTrackingInheritedFieldAccessTest.SpecificEntity.class)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@EnhancementOptions(inlineDirtyChecking = true)
class DirtyTrackingInheritedFieldAccessTest {

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var entity = new SpecificEntity();
			entity.id = 1L;
			entity.setBaseField( "before" );
			entity.setSomeField( "before" );
			session.persist( entity );
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void updateInheritedField(SessionFactoryScope scope) {
		assertUpdate( scope, SpecificEntity::setBaseField, SpecificEntity::getBaseField );
	}

	@Test
	void updateInheritedFieldThroughSuper(SessionFactoryScope scope) {
		assertUpdate( scope, SpecificEntity::setBaseFieldThroughSuper, SpecificEntity::getBaseField );
	}

	@Test
	void updateDeclaredField(SessionFactoryScope scope) {
		assertUpdate( scope, SpecificEntity::setSomeField, SpecificEntity::getSomeField );
	}

	private void assertUpdate(
			SessionFactoryScope scope,
			BiConsumer<SpecificEntity, String> setter,
			Function<SpecificEntity, String> getter) {
		scope.inTransaction( session -> {
			var entity = session.find( SpecificEntity.class, 1L );
			setter.accept( entity, "after" );
			session.flush();
			session.clear();
			assertThat( getter.apply( session.find( SpecificEntity.class, 1L ) ) ).isEqualTo( "after" );
		} );
	}

	@Test
	void trackInheritedField() {
		var entity = new SpecificEntity();
		entity.setBaseField( "after" );
		if ( entity instanceof SelfDirtinessTracker tracker ) {
			assertThat( tracker.$$_hibernate_getDirtyAttributes() ).containsExactly( "baseField" );
		}
	}

	@Test
	void leaveTransientFieldHidingInheritedFieldUnchanged() {
		var entity = new ShadowingEntity();
		entity.setTransientField( "after" );
		assertThat( entity.getTransientField() ).isEqualTo( "after" );
		assertThat( entity.getBaseField() ).isNull();
		if ( entity instanceof SelfDirtinessTracker tracker ) {
			assertThat( tracker.$$_hibernate_getDirtyAttributes() ).isEmpty();
		}
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		protected String baseField;

		public String getBaseField() {
			return baseField;
		}
	}

	@Entity(name = "SpecificEntity")
	public static class SpecificEntity extends BaseEntity {
		@Id
		private Long id;
		private String someField;

		public void setBaseField(String value) {
			baseField = value;
		}

		public void setBaseFieldThroughSuper(String value) {
			super.baseField = value;
		}

		public String getSomeField() {
			return someField;
		}

		public void setSomeField(String value) {
			someField = value;
		}
	}

	@Entity
	public static class ShadowingEntity extends BaseEntity {
		@Id
		private Long id;
		@Transient
		private String baseField;

		public String getTransientField() {
			return baseField;
		}

		public void setTransientField(String value) {
			baseField = value;
		}
	}
}
