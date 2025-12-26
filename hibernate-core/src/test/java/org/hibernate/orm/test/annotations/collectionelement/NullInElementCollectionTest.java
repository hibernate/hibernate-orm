/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.collectionelement;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@DomainModel(
		annotatedClasses = {
				NullInElementCollectionTest.Holder.class,
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-9456")
public class NullInElementCollectionTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Holder h = new Holder();
			h.id = 1L;
			h.name = "A";
			h.data.put( "a", null );
			s.persist( h );
			s.flush();
			s.clear();

			h = s.find( Holder.class, 1L );
			Assertions.assertEquals( 1, h.data.size() );
		} );
	}
	@Entity(name = "Holder")
	public static class Holder {
		@Id
		private Long id;
		private String name;
		@ElementCollection
		private Map<String, String> data = new HashMap<>();
	}
}
