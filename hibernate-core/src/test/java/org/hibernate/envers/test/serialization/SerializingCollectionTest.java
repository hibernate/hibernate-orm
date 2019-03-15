/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.CollectionRefIngEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SerializingCollectionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed1_id;
	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CollectionRefEdEntity.class, CollectionRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );
					final CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ing1 );

					this.ed1_id = ed1.getId();
					this.ing1_id = ing1.getId();
				}
		);
	}

	@DynamicTest
	public void testDetach() {
		inJPA(
				entityManager -> {
					CollectionRefIngEntity ing1 = entityManager.find( CollectionRefIngEntity.class, ing1_id );
					CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 1 );

					// First forcing loading of the collection
					assertThat( rev1.getReffering(), CollectionMatchers.hasSize( 1 ) );

					// Now serializing and de-serializing the
					rev1 = serializeDeserialize( rev1 );

					// And checking the collection again
					assertThat( rev1.getReffering(), contains( ing1 ) );
				}
		);
	}

	@SuppressWarnings({"unchecked"})
	private static <T> T serializeDeserialize(T o) {
		try {
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
		catch ( Exception e ) {
			throw new RuntimeException( "Failed to deserialize object", e );
		}
	}
}
