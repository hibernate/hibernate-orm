/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.serialization;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
