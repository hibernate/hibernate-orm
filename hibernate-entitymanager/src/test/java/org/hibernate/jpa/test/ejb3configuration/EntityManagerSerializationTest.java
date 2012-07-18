/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.ejb3configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Cat;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.Kitten;
import org.hibernate.jpa.test.NotSerializableClass;
import org.hibernate.jpa.test.Wallet;

/**
 * @author Emmanuel Bernard
 * @author Scott Marlow
 */
public class EntityManagerSerializationTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testSerialization() throws Exception {
		EntityManager em = entityManagerFactory().createEntityManager();
		//em.getTransaction().begin();
		//em.setFlushMode( FlushModeType.NEVER );
		Cat cat = new Cat();
		cat.setAge( 3 );
		cat.setDateOfBirth( new Date() );
		cat.setLength( 22 );
		cat.setName( "Kitty" );
		em.persist( cat );
		Item item = new Item();
		item.setName( "Train Ticket" );
		item.setDescr( "Paris-London" );
		em.persist( item );
		//em.getTransaction().commit();
		//em.getTransaction().begin();
		item.setDescr( "Paris-Bruxelles" );
		//em.getTransaction().commit();

		//fake the in container work
		( (HibernateEntityManager) em ).getSession().disconnect();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( em );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		em = (EntityManager) in.readObject();
		in.close();
		byteIn.close();
		//fake the in container work
		em.getTransaction().begin();
		item = em.find( Item.class, item.getName() );
		item.setDescr( item.getDescr() + "-Amsterdam" );
		cat = (Cat) em.createQuery( "select c from " + Cat.class.getName() + " c" ).getSingleResult();
		cat.setLength( 34 );
		em.flush();
		em.remove( item );
		em.remove( cat );
		em.flush();
		em.getTransaction().commit();

		em.close();
	}

	/**
	 * Add a non-serializable object to the EMF to ensure that the EM can be serialized even if its EMF is not serializable.
	 * This will ensure that the fix for HHH-6897 doesn't regress,
	 * @return
	 */
	@Override
	protected Map getConfig() {
		Map result = super.getConfig();
		result.put( "BaseEntityManagerFunctionalTestCase.getConfig_addedNotSerializableObject", new NotSerializableClass());
		return result;
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Item.class,
				Distributor.class,
				Wallet.class,
				Cat.class,
				Kitten.class
		};
	}
}
