/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.manytomany.compositepk;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner (extracted from ManyToManyTest authored by Emmanuel Bernard)
 */
@FailureExpectedWithNewMetamodel
public class ManyToManyCompositePkTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testDefaultCompositePk() throws Exception {
		Session s;
		Transaction tx;

		s = openSession();
		tx = s.beginTransaction();
		CatPk catPk = new CatPk();
		catPk.setName( "Minou" );
		catPk.setThoroughbred( "Persan" );
		Cat cat = new Cat();
		cat.setId( catPk );
		cat.setAge( 32 );
		Woman woman = new Woman();
		WomanPk womanPk = new WomanPk();
		womanPk.setFirstName( "Emma" );
		womanPk.setLastName( "Peel" );
		woman.setId( womanPk );
		woman.setCats( new HashSet<Cat>() );
		woman.getCats().add( cat );
		cat.setHumanContacts( new HashSet<Woman>() );
		cat.getHumanContacts().add( woman );
		s.persist( woman );
		s.persist( cat );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Cat sameCat = (Cat) s.get( Cat.class, cat.getId() );
		assertNotNull( sameCat );
		assertNotNull( sameCat.getHumanContacts() );
		assertEquals( 1, sameCat.getHumanContacts().size() );
		Woman sameWoman = sameCat.getHumanContacts().iterator().next();
		assertEquals( sameWoman.getId().getLastName(), woman.getId().getLastName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		sameWoman = (Woman) s.get( Woman.class, woman.getId() );
		assertNotNull( sameWoman );
		assertNotNull( sameWoman.getCats() );
		assertEquals( 1, sameWoman.getCats().size() );
		sameCat = sameWoman.getCats().iterator().next();
		assertEquals( cat.getAge(), sameCat.getAge() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositePk() throws Exception {
		Session s;
		Transaction tx;

		ManPk m1pk = new ManPk();
		m1pk.setElder( true );
		m1pk.setFirstName( "Lucky" );
		m1pk.setLastName( "Luke" );
		ManPk m2pk = new ManPk();
		m2pk.setElder( false );
		m2pk.setFirstName( "Joe" );
		m2pk.setLastName( "Dalton" );

		Man m1 = new Man();
		m1.setId( m1pk );
		m1.setCarName( "Jolly Jumper" );
		Man m2 = new Man();
		m2.setId( m2pk );

		WomanPk w1pk = new WomanPk();
		w1pk.setFirstName( "Ma" );
		w1pk.setLastName( "Dalton" );
		WomanPk w2pk = new WomanPk();
		w2pk.setFirstName( "Carla" );
		w2pk.setLastName( "Bruni" );

		Woman w1 = new Woman();
		w1.setId( w1pk );
		Woman w2 = new Woman();
		w2.setId( w2pk );

		Set<Woman> womens = new HashSet<Woman>();
		womens.add( w1 );
		m1.setWomens( womens );
		Set<Woman> womens2 = new HashSet<Woman>();
		womens2.add( w1 );
		womens2.add( w2 );
		m2.setWomens( womens2 );

		Set<Man> mens = new HashSet<Man>();
		mens.add( m1 );
		mens.add( m2 );
		w1.setMens( mens );
		Set<Man> mens2 = new HashSet<Man>();
		mens2.add( m2 );
		w2.setMens( mens2 );

		s = openSession();
		tx = s.beginTransaction();
		s.persist( m1 );
		s.persist( m2 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		m1 = (Man) s.load( Man.class, m1pk );
		assertFalse( m1.getWomens().isEmpty() );
		assertEquals( 1, m1.getWomens().size() );
		w1 = (Woman) s.load( Woman.class, w1pk );
		assertFalse( w1.getMens().isEmpty() );
		assertEquals( 2, w1.getMens().size() );

		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Man.class,
				Woman.class,
				Cat.class,
		};
	}
}
