/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.join;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.criterion.Restrictions;
import org.hibernate.mapping.Join;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class JoinTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testDefaultValue() throws Exception {
		Join join = (Join) metadata().getEntityBinding( Life.class.getName() ).getJoinClosureIterator().next();
		assertEquals( "ExtendedLife", join.getTable().getName() );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column();
		owner.setName( "LIFE_ID" );
		assertTrue( join.getTable().getPrimaryKey().containsColumn( owner ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Life life = new Life();
		life.duration = 15;
		life.fullDescription = "Long long description";
		s.persist( life );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from " + Life.class.getName() );
		life = (Life) q.uniqueResult();
		assertEquals( "Long long description", life.fullDescription );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositePK() throws Exception {
		Join join = (Join) metadata().getEntityBinding( Dog.class.getName() ).getJoinClosureIterator().next();
		assertEquals( "DogThoroughbred", join.getTable().getName() );
		org.hibernate.mapping.Column owner = new org.hibernate.mapping.Column();
		owner.setName( "OWNER_NAME" );
		assertTrue( join.getTable().getPrimaryKey().containsColumn( owner ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Dog dog = new Dog();
		DogPk id = new DogPk();
		id.name = "Thalie";
		id.ownerName = "Martine";
		dog.id = id;
		dog.weight = 30;
		dog.thoroughbredName = "Colley";
		s.persist( dog );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from Dog" );
		dog = (Dog) q.uniqueResult();
		assertEquals( "Colley", dog.thoroughbredName );
		tx.commit();
		s.close();
	}

	@Test
	public void testExplicitValue() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Death death = new Death();
		death.date = new Date();
		death.howDoesItHappen = "Well, haven't seen it";
		s.persist( death );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from " + Death.class.getName() );
		death = (Death) q.uniqueResult();
		assertEquals( "Well, haven't seen it", death.howDoesItHappen );
		s.delete( death );
		tx.commit();
		s.close();
	}

	@Test
	public void testManyToOne() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Life life = new Life();
		Cat cat = new Cat();
		cat.setName( "kitty" );
		cat.setStoryPart2( "and the story continues" );
		life.duration = 15;
		life.fullDescription = "Long long description";
		life.owner = cat;
		s.persist( life );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Criteria crit = s.createCriteria( Life.class );
		crit.createCriteria( "owner" ).add( Restrictions.eq( "name", "kitty" ) );
		life = (Life) crit.uniqueResult();
		assertEquals( "Long long description", life.fullDescription );
		s.delete( life.owner );
		s.delete( life );
		tx.commit();
		s.close();
	}
	
	@Test
	public void testReferenceColumnWithBacktics() throws Exception {
		Session s=openSession();
		s.beginTransaction();
		SysGroupsOrm g=new SysGroupsOrm();
		SysUserOrm u=new SysUserOrm();
		u.setGroups( new ArrayList<SysGroupsOrm>() );
		u.getGroups().add( g );
		s.save( g );
		s.save( u );
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	public void testUniqueConstaintOnSecondaryTable() throws Exception {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		Cat cat2 = new Cat();
		cat2.setStoryPart2( "My long story" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( cat );
			s.persist( cat2 );
			tx.commit();
			fail( "unique constraints violation on secondary table" );
		}
		catch (HibernateException e) {
			//success
			tx.rollback();
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testFetchModeOnSecondaryTable() throws Exception {
		Cat cat = new Cat();
		cat.setStoryPart2( "My long story" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		s.persist( cat );
		s.flush();
		s.clear();
		
		s.get( Cat.class, cat.getId() );
		//Find a way to test it, I need to define the secondary table on a subclass

		tx.rollback();
		s.close();
	}

	@Test
	public void testCustomSQL() throws Exception {
		Cat cat = new Cat();
		String storyPart2 = "My long story";
		cat.setStoryPart2( storyPart2 );
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		s.persist( cat );
		s.flush();
		s.clear();

		Cat c = (Cat) s.get( Cat.class, cat.getId() );
		assertEquals( storyPart2.toUpperCase(Locale.ROOT), c.getStoryPart2() );

		tx.rollback();
		s.close();
	}

	@Test
	public void testMappedSuperclassAndSecondaryTable() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		C c = new C();
		c.setAge( 12 );
		c.setCreateDate( new Date() );
		c.setName( "Bob" );
		s.persist( c );
		s.flush();
		s.clear();
		c= (C) s.get( C.class, c.getId() );
		assertNotNull( c.getCreateDate() );
		assertNotNull( c.getName() );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Life.class,
				Death.class,
				Cat.class,
				Dog.class,
				A.class,
				B.class,
				C.class,
				SysGroupsOrm.class,
				SysUserOrm.class
		};
	}
}
