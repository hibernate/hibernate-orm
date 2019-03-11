/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class CompositePkTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testDefaultCompositePk() {

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
		woman.setCats( new HashSet<>() );
		woman.getCats().add( cat );
		cat.setHumanContacts( new HashSet<>() );
		cat.getHumanContacts().add( woman );

		inTransaction(
				session -> {
					session.persist( woman );
				}
		);

		inTransaction(
				session -> {
					Cat sameCat = session.get( Cat.class, cat.getId() );
					assertNotNull( sameCat );
					assertNotNull( sameCat.getHumanContacts() );
					assertEquals( 1, sameCat.getHumanContacts().size() );
					Woman sameWoman = sameCat.getHumanContacts().iterator().next();
					assertEquals( sameWoman.getId().getLastName(), woman.getId().getLastName() );
				}
		);


		inTransaction(
				session -> {
					Woman sameWoman = session.get( Woman.class, woman.getId() );
					assertNotNull( sameWoman );
					assertNotNull( sameWoman.getCats() );
					assertEquals( 1, sameWoman.getCats().size() );
					Cat sameCat = sameWoman.getCats().iterator().next();
					assertEquals( cat.getAge(), sameCat.getAge() );
				}
		);
	}

	@Test
	public void testCompositePk() {
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

		Set<Woman> womens = new HashSet<>();
		womens.add( w1 );
		m1.setWomens( womens );
		Set<Woman> womens2 = new HashSet<>();
		womens2.add( w1 );
		womens2.add( w2 );
		m2.setWomens( womens2 );

		Set<Man> mens = new HashSet<>();
		mens.add( m1 );
		mens.add( m2 );
		w1.setMens( mens );
		Set<Man> mens2 = new HashSet<>();
		mens2.add( m2 );
		w2.setMens( mens2 );

		inTransaction(
				session -> {
					session.persist( m1 );
					session.persist( m2 );
				}
		);

		inTransaction(
				session -> {
					Man man = session.load( Man.class, m1pk );
					assertFalse( man.getWomens().isEmpty() );
					assertEquals( 1, man.getWomens().size() );
					Woman woman1 = session.load( Woman.class, w1pk );
					assertFalse( woman1.getMens().isEmpty() );
					assertEquals( 2, woman1.getMens().size() );
				}
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	protected void cleanupTestData() {
		inTransaction(
				session -> {
					List<Woman> women = session.createQuery( "from Woman" ).list();
					women.forEach( woman -> {
						woman.getMens().forEach( man -> session.delete( man ) );
						woman.getCats().forEach( cat -> session.delete( cat ) );
					} );

					List<Man> men = session.createQuery( "from Man" ).list();
					men.forEach( man -> session.delete( man ) );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Man.class,
				Woman.class,
				Cat.class,
		};
	}
}
