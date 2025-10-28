/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Troop.class,
		Soldier.class
})
public class DeleteOrphanTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDeleteOrphan(EntityManagerFactoryScope scope) throws Exception {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();

		scope.inTransaction(
				entityManager -> {
					disney.setName( "Disney" );
					mickey.setName( "Mickey" );
					disney.addSoldier( mickey );
					entityManager.persist( disney );
				}
		);

		Troop troop2 = scope.fromTransaction(
				entityManager -> {
					Troop troop = entityManager.find( Troop.class, disney.getId() );
					Hibernate.initialize( troop.getSoldiers() );
					return troop;
				}
		);
		Soldier soldier = troop2.getSoldiers().iterator().next();
		troop2.getSoldiers().remove( soldier );
		Troop troop3 = (Troop) deserialize( serialize( troop2 ) );

		scope.inTransaction(
				entityManager -> entityManager.merge( troop3 )
		);

		scope.inTransaction(
				entityManager -> {
					Soldier _soldier = entityManager.find( Soldier.class, mickey.getId() );
					assertNull( _soldier, "delete-orphan should work" );
					Troop _troop = entityManager.find( Troop.class, disney.getId() );
					entityManager.remove( _troop );
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
