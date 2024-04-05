/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;

@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtyCheckingWithEmbeddableExtedingAnotherEmbeddableAndTwiceRemovedNonVisibleGenericMappedSuperclassTest {

	@JiraKey("HHH-17034")
	@Test
	public void shouldNotBreakConstructor() {
		// Check the class has been enhanced
		assertThat( MyEntity.class )
				.hasDeclaredMethods(
						PERSISTENT_FIELD_READER_PREFIX + "embedded",
						PERSISTENT_FIELD_WRITER_PREFIX + "embedded"
				);

		assertThatCode( () -> new MyEntity( 0, "Magic the Gathering" ) )
				.doesNotThrowAnyException();
	}

	@MappedSuperclass
	public static abstract class MyAbstractEmbeddable {
	}

	@Embeddable
	public static class MyEmbeddable extends MyAbstractEmbeddable {

		@Column
		private String text;

		public MyEmbeddable() {
		}

		private MyEmbeddable(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@MappedSuperclass
	public static abstract class MyMappedSuperclass<C extends MyAbstractEmbeddable>
			extends MyNonVisibleGenericMappedSuperclass<C> {
	}

	@Entity(name = "myentity")
	public static class MyEntity extends MyMappedSuperclass<MyEmbeddable> {

		@Id
		private Integer id;

		public MyEntity() {
		}

		private MyEntity(Integer id, String text) {
			this.id = id;
			setEmbedded( new MyEmbeddable( text ) );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

	}

}
