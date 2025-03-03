/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.extractor.Extractors.resultOf;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_ENTRY_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.NEXT_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_GETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.PREVIOUS_SETTER_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_CLEAR_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_FIELD_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_GET_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_HAS_CHANGED_NAME;
import static org.hibernate.bytecode.enhance.spi.EnhancerConstants.TRACKER_SUSPEND_NAME;

import java.lang.reflect.Method;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtyCheckingWithEmbeddableAndNonVisibleGenericMappedSuperclassTest {

	@JiraKey("HHH-16832")
	@Test
	public void shouldNotBreakConstructor() {
		// This is the actual reproducer for HHH-16832,
		// other method are just to be consistent with tests in the same package.
		assertThatCode( () -> new MyEntity( 0, "Magic the Gathering" ) )
				.doesNotThrowAnyException();
	}

	@Test
	public void shouldDeclareFieldsInEntityClass() {
		assertThat( MyEntity.class )
				.hasDeclaredFields( ENTITY_ENTRY_FIELD_NAME, PREVIOUS_FIELD_NAME, NEXT_FIELD_NAME, TRACKER_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEntityClass() {
		assertThat( MyEntity.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "id", PERSISTENT_FIELD_WRITER_PREFIX + "id" )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "embedded", PERSISTENT_FIELD_WRITER_PREFIX + "embedded" )
				.hasDeclaredMethods( ENTITY_INSTANCE_GETTER_NAME, ENTITY_ENTRY_GETTER_NAME )
				.hasDeclaredMethods( PREVIOUS_GETTER_NAME, PREVIOUS_SETTER_NAME, NEXT_GETTER_NAME, NEXT_SETTER_NAME )
				.hasDeclaredMethods( TRACKER_HAS_CHANGED_NAME, TRACKER_CLEAR_NAME, TRACKER_SUSPEND_NAME, TRACKER_GET_NAME );
	}

	@Test
	public void shouldDeclareFieldsInEmbeddedClass() {
		assertThat( MyEmbeddable.class )
				.hasDeclaredFields( TRACKER_COMPOSITE_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEmbeddedClass() {
		assertThat( MyEmbeddable.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "text", PERSISTENT_FIELD_WRITER_PREFIX + "text" )
				.hasDeclaredMethods( TRACKER_COMPOSITE_SET_OWNER, TRACKER_COMPOSITE_CLEAR_OWNER );
	}

	@Test
	public void shouldCreateTheTracker() {
		MyEntity entity = new MyEntity( 0, "value1" );
		assertThat( entity )
				.extracting( NEXT_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( PREVIOUS_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( ENTITY_ENTRY_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( TRACKER_FIELD_NAME ).isInstanceOf( SimpleFieldTracker.class );
		assertThat( entity.getEmbedded() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME ).isInstanceOf( CompositeOwnerTracker.class );

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( true );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "embedded" } );
		assertThat( entity.getEmbedded() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME + ".names" ).isEqualTo( new String[] { "embedded" } );
	}

	@Test
	public void shouldResetTheTracker() throws Exception {
		MyEntity entity = new MyEntity( 1, "value1" );

		Method trackerClearMethod = MyEntity.class.getMethod( TRACKER_CLEAR_NAME );
		trackerClearMethod.invoke( entity );

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( false );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) ).isEqualTo( new String[0] );
	}

	@Test
	public void shouldUpdateTheTracker() throws Exception {
		MyEntity entity = new MyEntity( 2, "value1" );

		Method trackerClearMethod = MyEntity.class.getMethod( TRACKER_CLEAR_NAME );
		trackerClearMethod.invoke( entity );

		entity.getEmbedded().setText( "value2" );

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( true );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "embedded" } );

		trackerClearMethod.invoke( entity );

		entity.setEmbedded( new MyEmbeddable( "value3" ) );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "embedded" } );
		assertThat( entity.getEmbedded() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME + ".names" ).isEqualTo( new String[] { "embedded" } );
	}

	@Embeddable
	public static class MyEmbeddable {

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

	@Entity(name = "myentity")
	public static class MyEntity extends MyNonVisibleGenericMappedSuperclass<MyEmbeddable> {

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
