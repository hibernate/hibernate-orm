package org.hibernate.orm.test.sql.hand.identity;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Custom HBM insert SQL must be used while retrieving a post-insert identity.
///
/// @author Steve Ebersole
@DomainModel(xmlMappings = "org/hibernate/orm/test/sql/hand/identity/CustomIdentityInsert.hbm.xml")
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey("HHH-3735")
public class CustomIdentityInsertRoundTripTest {
	@Test
	void testCustomInsertAndGeneratedIdentity(SessionFactoryScope scope) {
		Long id = scope.fromTransaction( session -> {
			Item item = new Item();
			item.name = "custom insert";
			session.persist( item );
			session.flush();
			assertNotNull( item.id );
			return item.id;
		} );

		scope.inTransaction( session -> {
			Item loaded = session.find( Item.class, id );
			assertNotNull( loaded );
			assertEquals( id, loaded.id );
			// Only the custom INSERT applies upper(), so this also verifies which SQL ran.
			assertEquals( "CUSTOM INSERT", loaded.name );
			assertEquals( 1L, session.createQuery( "select count(*) from CustomIdentityItem", Long.class )
					.getSingleResult() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static class Item {
		private Long id;
		private String name;
	}
}
