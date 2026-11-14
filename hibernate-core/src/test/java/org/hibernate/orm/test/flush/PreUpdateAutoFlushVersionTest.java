package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = PreUpdateAutoFlushVersionTest.Item.class)
@SessionFactory
@JiraKey("HHH-21151")
class PreUpdateAutoFlushVersionTest {
	@Test
	void autoFlushAdvancesVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item();
			item.id = 1;
			session.persist( item );
			session.flush();

			item.name = "first";
			final var version = session.createSelectionQuery(
					"select version from Item where auditedName = :name", Integer.class )
					.setParameter( "name", "first" )
					.getSingleResult();
			assertEquals( 1, version );
			assertEquals( version, item.version );

			item.name = "second";
			session.flush();
			assertEquals( 2, item.version );
		} );
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		int id;
		@Version
		int version;
		String name;
		String auditedName;

		@PreUpdate
		void audit() {
			auditedName = name;
		}
	}
}
