package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = false)
public class FlexibleConstructorWithoutDirtyCheckingTest {
	@Test
	void assignmentBeforeSuperCallPreservesValueWithoutDirtyChecking() {
		assertThat( new FlexEntity( "test" ).title ).isEqualTo( "test" );
	}

	@Entity(name = "FlexEntityWithoutDirtyChecking")
	public static class FlexEntity {
		@Id
		private Long id;

		private String title;

		protected FlexEntity() {
		}

		public FlexEntity(String title) {
			this.title = title;
			super();
		}
	}
}
