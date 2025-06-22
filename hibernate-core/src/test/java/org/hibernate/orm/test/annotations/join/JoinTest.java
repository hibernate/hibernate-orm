/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.mapping.Join;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class JoinTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testDefaultValue() {
		Join join = metadata().getEntityBinding( Life.class.getName() ).getJoinClosure().get( 0 );
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
	public void testCompositePK() {
		Join join = metadata().getEntityBinding( Dog.class.getName() ).getJoinClosure().get( 0 );
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
	public void testExplicitValue() {
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
		s.remove( death );
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

		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Life> criteria = criteriaBuilder.createQuery( Life.class );
		Root<Life> root = criteria.from( Life.class );
		jakarta.persistence.criteria.Join<Object, Object> owner = root.join( "owner", JoinType.INNER );
		criteria.where( criteriaBuilder.equal( owner.get( "name" ), "kitty" ) );
		life = s.createQuery( criteria ).uniqueResult();

//		Criteria crit = s.createCriteria( Life.class );
//		crit.createCriteria( "owner" ).add( Restrictions.eq( "name", "kitty" ) );
//		life = (Life) crit.uniqueResult();
		assertEquals( "Long long description", life.fullDescription );
		s.remove( life.owner );
		s.remove( life );
		tx.commit();
		s.close();
	}

	@Test
	public void testReferenceColumnWithBacktics() {
		Session s=openSession();
		s.beginTransaction();
		SysGroupsOrm g=new SysGroupsOrm();
		SysUserOrm u=new SysUserOrm();
		u.setGroups( new ArrayList<>() );
		u.getGroups().add( g );
		s.persist( g );
		s.persist( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUniqueConstaintOnSecondaryTable() {
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
		catch (PersistenceException e) {
			try {
				assertTyping( ConstraintViolationException.class, e );
				//success
			}
			finally {
				tx.rollback();
			}
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testFetchModeOnSecondaryTable() {
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
	public void testCustomSQL() {
		Cat cat = new Cat();
		String storyPart2 = "My long story";
		cat.setStoryPart2( storyPart2 );
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		s.persist( cat );
		s.flush();
		s.clear();

		Cat c = s.get( Cat.class, cat.getId() );
		assertEquals( storyPart2.toUpperCase(Locale.ROOT), c.getStoryPart2() );

		tx.rollback();
		s.close();
	}

	@Test
	public void testMappedSuperclassAndSecondaryTable() {
		Session s = openSession( );
		s.getTransaction().begin();
		C c = new C();
		c.setAge( 12 );
		c.setCreateDate( new Date() );
		c.setName( "Bob" );
		s.persist( c );
		s.flush();
		s.clear();
		c= s.get( C.class, c.getId() );
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
