/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		Troop.class,
		Soldier.class
})
@SessionFactory
public class DeleteOrphanTest {
	@Test
	public void testDeleteOrphan(SessionFactoryScope scope) throws Exception {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();

		scope.inTransaction(
				session -> {
					disney.setName( "Disney" );
					mickey.setName( "Mickey" );
					disney.addSoldier( mickey );
					session.persist( disney );
				}
		);

		Troop troop2 = scope.fromTransaction(
				session -> {
					Troop troop = session.find( Troop.class, disney.getId() );
					Hibernate.initialize( troop.getSoldiers() );
					return troop;
				}
		);
		Soldier soldier = troop2.getSoldiers().iterator().next();
		troop2.getSoldiers().remove( soldier );
		Troop troop3 = (Troop) deserialize( serialize( troop2 ) );

		scope.inTransaction(
				session -> session.merge( troop3 )
		);

		scope.inTransaction(
				session -> {
					Soldier _soldier = session.find( Soldier.class, mickey.getId() );
					assertNull( _soldier, "delete-orphan should work" );
					Troop _troop = session.find( Troop.class, disney.getId() );
					session.remove( _troop );
				}
		);
	}

	private byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( object );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		return serialized;
	}

	private Object deserialize(byte[] serialized) throws IOException, ClassNotFoundException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		Object result = in.readObject();
		in.close();
		byteIn.close();
		return result;
	}
}
