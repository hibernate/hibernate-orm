/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.serialization;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {CollectionRefEdEntity.class, CollectionRefIngEntity.class})
public class SerializingCollection {
	private Integer ed1_id;
	private Integer ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );
			CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );

			em.persist( ed1 );
			em.persist( ing1 );

			ed1_id = ed1.getId();
			ing1_id = ing1.getId();
		} );
	}

	@Test
	public void testDetach(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			CollectionRefIngEntity ing1 = em.find( CollectionRefIngEntity.class, ing1_id );
			CollectionRefEdEntity rev1 = AuditReaderFactory.get( em ).find( CollectionRefEdEntity.class, ed1_id, 1 );

			// First forcing loading of the collection
			assertEquals( 1, rev1.getReffering().size() );

			// Now serializing and de-serializing the
			CollectionRefEdEntity deserialized = null;
			try {
				deserialized = serializeDeserialize( rev1 );
			}
			catch (Exception e) {
				fail( "Serialization / deserialization failed", e );
			}

			// And checking the collection again
			assertTrue( deserialized.getReffering().contains( ing1 ) );
			assertEquals( 1, deserialized.getReffering().size() );
		} );
	}

	@SuppressWarnings("unchecked")
	public static <T> T serializeDeserialize(T o) throws Exception {
		if ( o == null ) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( o );
		byte[] buffer = baos.toByteArray();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream( buffer );
		ObjectInputStream ois = new ObjectInputStream( bais );
		return (T) ois.readObject();
	}
}
