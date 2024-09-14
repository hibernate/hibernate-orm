/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.manytomanyassociationclass.nestedreference;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/manytomanyassociationclass/nestedreference/Item.hbm.xml" }
)
@SessionFactory
public class ItemSelfReferenceTest {
	@Test
	public void testSimpleCreateAndDelete(SessionFactoryScope scope) {
		Item item = new Item( "turin", "tiger" );
		scope.inTransaction(
				session -> {
					session.persist( item );
				}
		);

		scope.inTransaction(
				session -> {
					Item innerItem = session.getReference(Item.class, item.getId());
					session.remove(innerItem);
				}
		);
	}
}
