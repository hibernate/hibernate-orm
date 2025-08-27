/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test some composite id functionalities
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class,
				Channel.class,
				TvMagazin.class,
				TvProgramIdClass.class,
				TvProgram.class,
				Presenter.class,
				Order.class,
				Product.class,
				OrderLine.class,
				OrderLinePk.class,
				LittleGenius.class,
				A.class,
				B.class,
				C.class,
				SomeEntity.class
		}
)
@SessionFactory
public class CompositeIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneInCompositePk(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					B b = new B();
					C c = new C();
					session.persist( b );
					session.persist( c );
					A a = new A();
					a.setAId( new AId() );
					a.getAId().setB( b );
					a.getAId().setC( c );
					session.persist( a );
					session.flush();
					session.clear();

					a = session.get( A.class, a.getAId() );
					assertEquals( b.getId(), a.getAId().getB().getId() );
				}
		);
	}

	@Test
	public void testUpdateCompositeIdFkAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "update Child c set c.parent1 = null" ).executeUpdate();
				}
		);
	}


	/**
	 * This feature is not supported by the EJB3
	 * this is an hibernate extension
	 */
	@Test
	public void testManyToOneInCompositePk(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParentPk ppk = new ParentPk();
					ppk.setFirstName( "Emmanuel" );
					ppk.setLastName( "Bernard" );
					Parent p = new Parent();
					p.id = ppk;
					session.persist( p );
					ChildPk cpk = new ChildPk();
					cpk.parent = p;
					cpk.nthChild = 1;
					Child c = new Child();
					c.id = cpk;
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "select c from Child c where c.id.nthChild = :nth" );
					q.setParameter( "nth", 1 );
					List results = q.list();
					assertEquals( 1, results.size() );
					Child c = (Child) results.get( 0 );
					assertNotNull( c );
					assertNotNull( c.id.parent );
					//FIXME mke it work in unambigious cases
					//		assertNotNull(c.id.parent.id);
					//		assertEquals(p.id.getFirstName(), c.id.parent.id.getFirstName());
					session.remove( c );
					session.remove( c.id.parent );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10476")
	public void testManyToOneInCompositePkInPC(SessionFactoryScope scope) {
		ParentPk ppk = new ParentPk();
		ChildPk cpk = new ChildPk();
		scope.inTransaction(
				session -> {
					ppk.setFirstName( "Emmanuel" );
					ppk.setLastName( "Bernard" );
					Parent p = new Parent();
					p.id = ppk;
					session.persist( p );
					cpk.parent = p;
					cpk.nthChild = 1;
					Child c = new Child();
					c.id = cpk;
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, ppk );
					// p.id should be ppk.
					assertSame( ppk, p.id );
				}
		);

		scope.inTransaction(
				session -> {
					Child c = session.get( Child.class, cpk );
					// c.id should be cpk
					assertSame( cpk, c.id );
					// only Child should be in PC (c.id.parent should not be in PC)
					assertTrue( session.getPersistenceContext().isEntryFor( c ) );
					assertFalse( session.getPersistenceContext().isEntryFor( c.id.parent ) );

				}
		);
	}

	/**
	 * This feature is not supported by the EJB3
	 * this is an hibernate extension
	 */
	@Test
	public void testManyToOneInCompositePkAndSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParentPk ppk = new ParentPk();
					ppk.setFirstName( "Emmanuel" );
					ppk.setLastName( "Bernard" );
					Parent p = new Parent();
					p.id = ppk;
					session.persist( p );
					ChildPk cpk = new ChildPk();
					cpk.parent = p;
					cpk.nthChild = 1;
					LittleGenius c = new LittleGenius();
					c.particularSkill = "Human Annotation parser";
					c.id = cpk;
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "select c from Child c where c.id.nthChild = :nth" );
					q.setParameter( "nth", 1 );
					List results = q.list();
					assertEquals( 1, results.size() );
					Child c = (LittleGenius) results.get( 0 );
					assertNotNull( c );
					assertNotNull( c.id.parent );
					//FIXME mke it work in unambigious cases
//		assertNotNull(c.id.parent.id);
//		assertEquals(p.id.getFirstName(), c.id.parent.id.getFirstName());
					session.remove( c );
					session.remove( c.id.parent );
				}
		);
	}

	@Test
	public void testManyToOneInCompositeId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Channel channel = new Channel();
					session.persist( channel );
					Presenter pres = new Presenter();
					pres.name = "Casimir";
					session.persist( pres );
					TvMagazinPk pk = new TvMagazinPk();
					TvMagazin mag = new TvMagazin();
					mag.time = new Date();
					mag.id = pk;
					pk.channel = channel;
					pk.presenter = pres;
					session.persist( mag );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					mag = (TvMagazin) session.createQuery( "from TvMagazin mag" ).uniqueResult();
					assertNotNull( mag.id );
					assertNotNull( mag.id.channel );
					assertEquals( channel.id, mag.id.channel.id );
					assertNotNull( mag.id.presenter );
					assertEquals( pres.name, mag.id.presenter.name );
					session.remove( mag );
					session.remove( mag.id.channel );
					session.remove( mag.id.presenter );
				}
		);
	}

	@Test
	public void testManyToOneInCompositeIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order();
					session.persist( order );
					Product product = new Product();
					product.name = "small car";
					session.persist( product );
					OrderLine orderLine = new OrderLine();
					orderLine.order = order;
					orderLine.product = product;
					session.persist( orderLine );
					session.flush();
					session.clear();

					orderLine = (OrderLine) session.createQuery( "select ol from OrderLine ol" ).uniqueResult();
					assertNotNull( orderLine.order );
					assertEquals( order.id, orderLine.order.id );
					assertNotNull( orderLine.product );
					assertEquals( product.name, orderLine.product.name );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10476")
	public void testManyToOneInCompositeIdClassInPC(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order();
					session.persist( order );
					Product product = new Product();
					product.name = "small car";
					session.persist( product );
					OrderLine orderLine = new OrderLine();
					orderLine.order = order;
					orderLine.product = product;
					session.persist( orderLine );
					session.flush();
					session.clear();

					session.clear();
					OrderLinePk orderLinePK = new OrderLinePk();
					orderLinePK.order = orderLine.order;
					orderLinePK.product = orderLine.product;
					orderLine = session.get( OrderLine.class, orderLinePK );
					assertTrue( orderLine.order != orderLinePK.order );
					assertTrue( orderLine.product != orderLinePK.product );
					assertTrue( session.getPersistenceContext().isEntryFor( orderLine ) );
					assertTrue( session.getPersistenceContext().isEntryFor( orderLine.order ) );
					assertTrue( session.getPersistenceContext().isEntryFor( orderLine.product ) );
					assertFalse( session.getPersistenceContext().isEntryFor( orderLinePK.order ) );
					assertFalse( session.getPersistenceContext().isEntryFor( orderLinePK.product ) );

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10476")
	public void testGetWithUpdatedDetachedEntityInCompositeID(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Channel channel = new Channel();
					Presenter presenter = new Presenter();
					presenter.name = "Jane";
					TvMagazin tvMagazin = new TvMagazin();
					tvMagazin.id = new TvMagazinPk();
					tvMagazin.id.channel = channel;
					tvMagazin.id.presenter = presenter;
					session.persist( channel );
					session.persist( presenter );
					session.persist( tvMagazin );
					session.flush();

					session.clear();
					// update channel
					channel.name = "chnl";
					TvMagazinPk pkNew = new TvMagazinPk();
					// set pkNew.channel to the unmerged copy.
					pkNew.channel = channel;
					pkNew.presenter = presenter;
					// the following fails because there is already a managed channel
					tvMagazin = session.get( TvMagazin.class, pkNew );
					channel = session.get( Channel.class, channel.id );
					assertNull( channel.name );
					session.flush();
					session.clear();

					// make sure that channel.name is still null
					channel = session.get( Channel.class, channel.id );
					assertNull( channel.name );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10476")
	public void testGetWithDetachedEntityInCompositeIDWithManagedCopy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Channel channel = new Channel();
					Presenter presenter = new Presenter();
					presenter.name = "Jane";
					TvMagazin tvMagazin = new TvMagazin();
					tvMagazin.id = new TvMagazinPk();
					tvMagazin.id.channel = channel;
					tvMagazin.id.presenter = presenter;
					session.persist( channel );
					session.persist( presenter );
					session.persist( tvMagazin );
					session.flush();

					session.clear();
					// merge channel to put channel back in PersistenceContext
					session.merge( channel );
					TvMagazinPk pkNew = new TvMagazinPk();
					// set pkNew.channel to the unmerged copy.
					pkNew.channel = channel;
					pkNew.presenter = presenter;
					// the following fails because there is already a managed channel
					tvMagazin = session.get( TvMagazin.class, pkNew );
				}
		);
	}

	@Test
	public void testSecondaryTableWithCompositeId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Channel channel = new Channel();
					session.persist( channel );
					Presenter pres = new Presenter();
					pres.name = "Tim Russet";
					session.persist( pres );
					TvMagazinPk pk = new TvMagazinPk();
					TvProgram program = new TvProgram();
					program.time = new Date();
					program.id = pk;
					program.text = "Award Winning Programming";
					pk.channel = channel;
					pk.presenter = pres;
					session.persist( program );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					program = (TvProgram) session.createQuery( "from TvProgram pr" ).uniqueResult();
					assertNotNull( program.id );
					assertNotNull( program.id.channel );
					assertEquals( channel.id, program.id.channel.id );
					assertNotNull( program.id.presenter );
					assertNotNull( program.text );
					assertEquals( pres.name, program.id.presenter.name );
					session.remove( program );
					session.remove( program.id.channel );
					session.remove( program.id.presenter );
				}
		);
	}

	@Test
	public void testSecondaryTableWithIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Channel channel = new Channel();
					session.persist( channel );
					Presenter pres = new Presenter();
					pres.name = "Bob";
					session.persist( pres );
					TvProgramIdClass program = new TvProgramIdClass();
					program.time = new Date();
					program.channel = channel;
					program.presenter = pres;
					program.text = "Jump the shark programming";
					session.persist( program );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					program = (TvProgramIdClass) session.createQuery( "from TvProgramIdClass pr" ).uniqueResult();
					assertNotNull( program.channel );
					assertEquals( channel.id, program.channel.id );
					assertNotNull( program.presenter );
					assertNotNull( program.text );
					assertEquals( pres.name, program.presenter.name );
					session.remove( program );
					session.remove( program.channel );
					session.remove( program.presenter );
				}
		);
	}

	@Test
	public void testQueryInAndComposite(SessionFactoryScope scope) {

		scope.inTransaction(
				s -> {
					createData( s );
					s.flush();
					List ids = new ArrayList<SomeEntityId>( 2 );
					ids.add( new SomeEntityId( 1, 12 ) );
					ids.add( new SomeEntityId( 10, 23 ) );

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<SomeEntity> criteria = criteriaBuilder.createQuery( SomeEntity.class );
					Root<SomeEntity> root = criteria.from( SomeEntity.class );
					CriteriaBuilder.In<Object> inPredicate = criteriaBuilder.in( root.get( "id" ) );
					criteria.where( criteriaBuilder.or( inPredicate.value( ids ) ) );
					List list = s.createQuery( criteria ).list();

//					Criteria criteria = s.createCriteria( SomeEntity.class );
//					Disjunction disjunction = Restrictions.disjunction();
//
//					disjunction.add( Restrictions.in( "id", ids  ) );
//					criteria.add( disjunction );
//
//					List list = criteria.list();
					assertEquals( 2, list.size() );
				}
		);
	}

	@Test
	public void testQueryInAndCompositeWithHQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					createData( session );
					session.flush();
					List<SomeEntityId> ids = new ArrayList<>( 2 );
					ids.add( new SomeEntityId( 1, 12 ) );
					ids.add( new SomeEntityId( 10, 23 ) );
					ids.add( new SomeEntityId( 10, 22 ) );
					Query query = session.createQuery( "from SomeEntity e where e.id in (:idList)" );
					query.setParameterList( "idList", ids );
					List list = query.list();
					assertEquals( 3, list.size() );
				}
		);
	}

	private void createData(Session s) {
		SomeEntity someEntity = new SomeEntity();
		someEntity.setId( new SomeEntityId() );
		someEntity.getId().setId( 1 );
		someEntity.getId().setVersion( 11 );
		someEntity.setProp( "aa" );
		s.persist( someEntity );

		someEntity = new SomeEntity();
		someEntity.setId( new SomeEntityId() );
		someEntity.getId().setId( 1 );
		someEntity.getId().setVersion( 12 );
		someEntity.setProp( "bb" );
		s.persist( someEntity );

		someEntity = new SomeEntity();
		someEntity.setId( new SomeEntityId() );
		someEntity.getId().setId( 10 );
		someEntity.getId().setVersion( 21 );
		someEntity.setProp( "cc1" );
		s.persist( someEntity );

		someEntity = new SomeEntity();
		someEntity.setId( new SomeEntityId() );
		someEntity.getId().setId( 10 );
		someEntity.getId().setVersion( 22 );
		someEntity.setProp( "cc2" );
		s.persist( someEntity );

		someEntity = new SomeEntity();
		someEntity.setId( new SomeEntityId() );
		someEntity.getId().setId( 10 );
		someEntity.getId().setVersion( 23 );
		someEntity.setProp( "cc3" );
		s.persist( someEntity );
	}
}
