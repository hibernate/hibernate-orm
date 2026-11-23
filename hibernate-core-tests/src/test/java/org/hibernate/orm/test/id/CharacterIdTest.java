package org.hibernate.orm.test.id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Character identifiers must bootstrap and round-trip using legacy XML mappings.
///
/// @author Steve Ebersole
@DomainModel(xmlMappings = "org/hibernate/orm/test/id/CharacterId.hbm.xml")
@SessionFactory
@JiraKey("HHH-2297")
public class CharacterIdTest {
	@Test
	void testCharacterIdentifiers(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			PrimitiveCharacterEntity primitive = new PrimitiveCharacterEntity();
			primitive.id = 'A';
			primitive.name = "primitive";
			session.persist( primitive );
			BoxedCharacterEntity boxed = new BoxedCharacterEntity();
			boxed.id = 'B';
			boxed.name = "boxed";
			session.persist( boxed );
			session.flush();
		} );

		scope.inTransaction( session -> {
			PrimitiveCharacterEntity primitive = session.find( PrimitiveCharacterEntity.class, 'A' );
			assertNotNull( primitive );
			assertEquals( 'A', primitive.id );
			assertEquals( "primitive", primitive.name );
			BoxedCharacterEntity boxed = session.find( BoxedCharacterEntity.class, 'B' );
			assertNotNull( boxed );
			assertEquals( Character.valueOf( 'B' ), boxed.id );
			assertEquals( "boxed", boxed.name );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static class PrimitiveCharacterEntity {
		private char id;
		private String name;
	}

	public static class BoxedCharacterEntity {
		private Character id;
		private String name;
	}
}
