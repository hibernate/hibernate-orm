/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.entityname;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(xmlMappings = { "org/hibernate/orm/test/entityname/MyEntity.hbm.xml" })
@SessionFactory
public class MultipleMappingsTest {

	@Test
	public void test(SessionFactoryScope scope) {
		MyEntity entity1 = new MyEntity();
		entity1.setName("One");
		MyEntity entity2 = new MyEntity();
		entity2.setName("Two");
		entity1.setOther(entity2);
		entity2.setOther(entity1);
		scope.inTransaction(s -> {
			s.persist("EntityWon", entity1 );
			s.persist("EntityToo", entity2 );
		});
		scope.inTransaction(s -> {
			MyEntity entity = (MyEntity) s.get("EntityWon", entity1.getId());
			assertNotNull( entity );
			assertEquals( "One", entity.getName() );
			MyEntity other = entity.getOther();
			assertEquals( "Two", other.getName() );
			assertEquals( other.getId(), entity2.getId() );
			entity.setOther( null );
			s.remove( other );
			s.remove( entity );
		});
	}
}
