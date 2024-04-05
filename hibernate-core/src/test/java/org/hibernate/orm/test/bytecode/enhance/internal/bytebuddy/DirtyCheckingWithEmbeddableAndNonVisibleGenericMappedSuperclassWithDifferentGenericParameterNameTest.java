/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_CLEAR_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_GET_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_HAS_CHANGED_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_SUSPEND_NAME;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@JiraKey("HHH-17035")
@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtyCheckingWithEmbeddableAndNonVisibleGenericMappedSuperclassWithDifferentGenericParameterNameTest {

	@Test
	public void shouldDeclareMethodsInEntityClass() {
		// The test really is just about checking that bytecode enhancement
		// (which will run when we load the class for the first time)
		// does not fail with a StackOverflowError.
		// But it doesn't hurt to perform a few additional checks.
		assertThat( MyEntity.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "id", PERSISTENT_FIELD_WRITER_PREFIX + "id" )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "embedded", PERSISTENT_FIELD_WRITER_PREFIX + "embedded" )
				.hasDeclaredMethods( ENTITY_INSTANCE_GETTER_NAME, ENTITY_ENTRY_GETTER_NAME )
				.hasDeclaredMethods( PREVIOUS_GETTER_NAME, PREVIOUS_SETTER_NAME, NEXT_GETTER_NAME, NEXT_SETTER_NAME )
				.hasDeclaredMethods( TRACKER_HAS_CHANGED_NAME, TRACKER_CLEAR_NAME, TRACKER_SUSPEND_NAME, TRACKER_GET_NAME );
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
	// The key to reproducing the problem is to use a different name for the generic parameter
	// in this class than in the superclass.
	// Note that, as in other similar tests, the field of the superclass needs
	// to not be visible from the subclass in other to reproduce the problem.
	public static abstract class MyLevel2GenericMappedSuperclass<T extends MyAbstractEmbeddable>
			extends MyNonVisibleGenericMappedSuperclass<T> {
	}

	@Entity(name = "myentity")
	public static class MyEntity extends MyLevel2GenericMappedSuperclass<MyEmbeddable> {

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
