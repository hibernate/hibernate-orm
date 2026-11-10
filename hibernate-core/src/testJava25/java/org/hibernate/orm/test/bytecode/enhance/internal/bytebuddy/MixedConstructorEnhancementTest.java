package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Constructors in the same enhanced class may initialize fields on either side
 * of the superclass constructor invocation.
 */
@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class MixedConstructorEnhancementTest {
	@Test
	void entitySupportsBothConstructorStylesInTheSameClass() {
		var conventional = new MixedEntity( (CharSequence) "after" );
		var flexible = new MixedEntity( "before" );

		assertThat( conventional.title ).isEqualTo( "after" );
		assertThat( flexible.title ).isEqualTo( "before" );
		assertThat( ( (SelfDirtinessTracker) conventional ).$$_hibernate_getDirtyAttributes() )
				.containsExactly( "title" );
		assertThat( ( (SelfDirtinessTracker) flexible ).$$_hibernate_getDirtyAttributes() )
				.containsExactly( "title" );
	}

	@Test
	void embeddableSupportsBothConstructorStylesInTheSameClass() {
		var conventional = new MixedAddress( (CharSequence) "after" );
		var flexible = new MixedAddress( "before" );

		assertThat( conventional.street ).isEqualTo( "after" );
		assertThat( flexible.street ).isEqualTo( "before" );
	}

	@Entity(name = "MixedConstructorEntity")
	public static class MixedEntity {
		@Id
		private Long id;

		private String title;

		protected MixedEntity() {
		}

		public MixedEntity(String title) {
			this.title = title;
			super();
		}

		public MixedEntity(CharSequence title) {
			super();
			this.title = title.toString();
		}
	}

	@Embeddable
	public static class MixedAddress {
		private String street;

		protected MixedAddress() {
		}

		public MixedAddress(String street) {
			this.street = street;
			super();
		}

		public MixedAddress(CharSequence street) {
			super();
			this.street = street.toString();
		}
	}
}
