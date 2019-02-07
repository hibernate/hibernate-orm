/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to operations on a PersistentBag.
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class PersistentBagTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHmbMappingFiles() {
		return new String[] { "collection/bag/Mappings.hbm.xml" };
	}

	@Test
	public void testWriteMethodDirtying() {
		BagOwner parent = new BagOwner( "root" );
		BagOwner child = new BagOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		BagOwner otherChild = new BagOwner( "c2" );

		inTransaction(
				session -> {
					session.save( parent );
					session.flush();
					// at this point, the list on parent has now been replaced with a PersistentBag...
					PersistentBag children = (PersistentBag) parent.getChildren();

					assertFalse( children.remove( otherChild ) );
					assertFalse( children.isDirty() );

					ArrayList otherCollection = new ArrayList();
					otherCollection.add( child );
					assertFalse( children.retainAll( otherCollection ) );
					assertFalse( children.isDirty() );

					otherCollection = new ArrayList();
					otherCollection.add( otherChild );
					assertFalse( children.removeAll( otherCollection ) );
					assertFalse( children.isDirty() );

					children.clear();
					session.delete( child );
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.delete( parent );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = AbstractHANADialect.class, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testMergePersistentEntityWithNewOneToManyElements() {
		// todo (6.0): when post insert id generation will be managed change the id generator mapping back to "native"
		final Order o = new Order();

		inTransaction(
				session -> {
					session.persist( o );
				} );

		inTransaction(
				session -> {
					Order order = session.get( Order.class, o.getId() );
					Item item1 = new Item();
					item1.setName( "i1" );
					Item item2 = new Item();
					item2.setName( "i2" );
					order.addItem( item1 );
					order.addItem( item2 );
					session.merge( order );
					//session.flush();
				}
		);

		inTransaction(
				session -> {
					Order order = session.get( Order.class, o.getId() );
					assertEquals( 2, order.getItems().size() );
					session.delete( order );
				}
		);
	}
}
