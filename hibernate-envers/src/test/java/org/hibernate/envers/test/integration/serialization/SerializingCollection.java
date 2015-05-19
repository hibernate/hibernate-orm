/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.CollectionRefIngEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SerializingCollection extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CollectionRefEdEntity.class, CollectionRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );

		CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ing1 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ing1_id = ing1.getId();
	}

	@Test
	public void testDetach() throws Exception {
		CollectionRefIngEntity ing1 = getEntityManager().find( CollectionRefIngEntity.class, ing1_id );
		CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 1 );

		// First forcing loading of the collection
		assert rev1.getReffering().size() == 1;

		// Now serializing and de-serializing the
		rev1 = serializeDeserialize( rev1 );

		// And checking the colleciton again
		assert rev1.getReffering().contains( ing1 );
		assert rev1.getReffering().size() == 1;

	}

	@SuppressWarnings({"unchecked"})
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
