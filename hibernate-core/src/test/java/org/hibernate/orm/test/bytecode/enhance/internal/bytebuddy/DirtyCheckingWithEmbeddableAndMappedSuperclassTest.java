/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy.DirtyCheckingWithEmbeddableAndMappedSuperclassTest.CardGame;

import java.lang.reflect.Method;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;


@BytecodeEnhanced(testEnhancedClasses = CardGame.class)
@EnhancementOptions(inlineDirtyChecking = true)
@JiraKey("HHH-13759")
public class DirtyCheckingWithEmbeddableAndMappedSuperclassTest {

	@Test
	public void shouldDeclareFieldsInEntityClass() {
		assertThat( CardGame.class )
				.hasDeclaredFields( ENTITY_ENTRY_FIELD_NAME, PREVIOUS_FIELD_NAME, NEXT_FIELD_NAME, TRACKER_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEntityClass() {
		assertThat( CardGame.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "id", PERSISTENT_FIELD_WRITER_PREFIX + "id" )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "name", PERSISTENT_FIELD_WRITER_PREFIX + "name" )
				.hasDeclaredMethods( ENTITY_INSTANCE_GETTER_NAME, ENTITY_ENTRY_GETTER_NAME )
				.hasDeclaredMethods( PREVIOUS_GETTER_NAME, PREVIOUS_SETTER_NAME, NEXT_GETTER_NAME, NEXT_SETTER_NAME )
				.hasDeclaredMethods( TRACKER_HAS_CHANGED_NAME, TRACKER_CLEAR_NAME, TRACKER_SUSPEND_NAME, TRACKER_GET_NAME );
	}

	@Test
	public void shouldDeclareFieldsInEmbeddedClass() {
		assertThat( Component.class )
				.hasDeclaredFields( TRACKER_COMPOSITE_FIELD_NAME );
	}

	@Test
	public void shouldDeclareMethodsInEmbeddedClass() {
		assertThat(Component.class )
				.hasDeclaredMethods( PERSISTENT_FIELD_READER_PREFIX + "component", PERSISTENT_FIELD_WRITER_PREFIX + "component" )
				.hasDeclaredMethods( TRACKER_COMPOSITE_SET_OWNER, TRACKER_COMPOSITE_CLEAR_OWNER );
	}

	@Test
	public void shouldCreateTheTracker() throws Exception {
		CardGame entity = new CardGame( "MTG", "Magic the Gathering" );
		assertThat( entity )
				.extracting( NEXT_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( PREVIOUS_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( ENTITY_ENTRY_FIELD_NAME ).isNull();
		assertThat( entity )
				.extracting( TRACKER_FIELD_NAME ).isInstanceOf( SimpleFieldTracker.class );
		assertThat( entity.getFirstPlayerToken() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME ).isInstanceOf( CompositeOwnerTracker.class);

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( true );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "name", "firstPlayerToken" } );
		assertThat( entity.getFirstPlayerToken() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME + ".names" ).isEqualTo( new String[] { "firstPlayerToken" } );
	}

	@Test
	public void shouldResetTheTracker() throws Exception {
		CardGame entity = new CardGame( "7WD", "7 Wonders duel" );

		Method trackerClearMethod = CardGame.class.getMethod( TRACKER_CLEAR_NAME );
		trackerClearMethod.invoke( entity );

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( false );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) ).isEqualTo( new String[0] );
	}

	@Test
	public void shouldUpdateTheTracker() throws Exception {
		CardGame entity = new CardGame( "SPL", "Splendor" );

		Method trackerClearMethod = CardGame.class.getMethod( TRACKER_CLEAR_NAME );
		trackerClearMethod.invoke( entity );

		entity.setName( "Splendor: Cities of Splendor" );

		assertThat( entity ).extracting( resultOf( TRACKER_HAS_CHANGED_NAME ) ).isEqualTo( true );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "name", "firstPlayerToken" } );

		trackerClearMethod.invoke( entity );

		entity.setFirstPlayerToken( new Component( "FIRST PLAYER!!!!!!!!" ) );
		assertThat( entity ).extracting( resultOf( TRACKER_GET_NAME ) )
				.isEqualTo( new String[] { "firstPlayerToken" } );
		assertThat( entity.getFirstPlayerToken() )
				.extracting( TRACKER_COMPOSITE_FIELD_NAME + ".names" ).isEqualTo( new String[] { "firstPlayerToken" } );
	}

	@MappedSuperclass
	public static abstract class TableTopGame {

		@Embedded
		private Component firstPlayerToken;

		public Component getFirstPlayerToken() {
			return firstPlayerToken;
		}

		public void setFirstPlayerToken(Component firstPlayerToken) {
			this.firstPlayerToken = firstPlayerToken;
		}
	}

	@Embeddable
	public static class Component {

		@Column(name = "first_player_token")
		private String component;

		public Component() {
		}

		private Component(String component) {
			this.component = component;
		}

		public String getComponent() {
			return component;
		}

		public void setComponent(String component) {
			this.component = component;
		}
	}

	@Entity(name = "CardGame")
	public static class CardGame extends TableTopGame {

		@Id
		private String id;
		private String name;

		public CardGame() {
		}

		private CardGame(String id, String name) {
			this.id = id;
			this.name = name;
			setFirstPlayerToken( createEmbeddedValue( name ) );
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			setFirstPlayerToken( createEmbeddedValue( name ) );
		}

		private Component createEmbeddedValue(String name) {
			return new Component( name + " first player token");
		}
	}

}
