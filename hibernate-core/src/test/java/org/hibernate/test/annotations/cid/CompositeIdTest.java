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
package org.hibernate.test.annotations.cid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * test some composite id functionalities
 *
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel(
		message = "Part of the problem is how some of the composite ids are defined.  For example, " +
				"quite a few map a logical `@Id @ManyToOne` combo without the @ManyToOne.  Fixing those " +
				"leads to an not being able to bind the org.hibernate.test.annotations.cid.A entity; it " +
				"has composite id made up of 2 @OneToOnes (need to research that, why not @ManyToOne?)."
)
public class CompositeIdTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOneToOneInCompositePk() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		B b = new B();
		C c = new C();
		s.persist( b );
		s.persist( c );
		A a = new A();
		a.setAId( new AId() );
		a.getAId().setB( b );
		a.getAId().setC( c );
		s.persist( a );
		s.flush();
		s.clear();

		a = (A) s.get(A.class, a.getAId() );
		assertEquals( b.getId(), a.getAId().getB().getId() );

		tx.rollback();
		s.close();
	}


	/**
	 * This feature is not supported by the EJB3
	 * this is an hibernate extension
	 */
	@Test
	public void testManyToOneInCompositePk() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		ParentPk ppk = new ParentPk();
		ppk.setFirstName( "Emmanuel" );
		ppk.setLastName( "Bernard" );
		Parent p = new Parent();
		p.id = ppk;
		s.persist( p );
		ChildPk cpk = new ChildPk();
		cpk.parent = p;
		cpk.nthChild = 1;
		Child c = new Child();
		c.id = cpk;
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "select c from Child c where c.id.nthChild = :nth" );
		q.setInteger( "nth", 1 );
		List results = q.list();
		assertEquals( 1, results.size() );
		c = (Child) results.get( 0 );
		assertNotNull( c );
		assertNotNull( c.id.parent );
		//FIXME mke it work in unambigious cases
		//		assertNotNull(c.id.parent.id);
		//		assertEquals(p.id.getFirstName(), c.id.parent.id.getFirstName());
		s.delete( c );
		s.delete( c.id.parent );
		tx.commit();
		s.close();
	}

	/**
	 * This feature is not supported by the EJB3
	 * this is an hibernate extension
	 */
	@Test
	public void testManyToOneInCompositePkAndSubclass() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		ParentPk ppk = new ParentPk();
		ppk.setFirstName( "Emmanuel" );
		ppk.setLastName( "Bernard" );
		Parent p = new Parent();
		p.id = ppk;
		s.persist( p );
		ChildPk cpk = new ChildPk();
		cpk.parent = p;
		cpk.nthChild = 1;
		LittleGenius c = new LittleGenius();
		c.particularSkill = "Human Annotation parser";
		c.id = cpk;
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "select c from Child c where c.id.nthChild = :nth" );
		q.setInteger( "nth", 1 );
		List results = q.list();
		assertEquals( 1, results.size() );
		c = (LittleGenius) results.get( 0 );
		assertNotNull( c );
		assertNotNull( c.id.parent );
		//FIXME mke it work in unambigious cases
//		assertNotNull(c.id.parent.id);
//		assertEquals(p.id.getFirstName(), c.id.parent.id.getFirstName());
		s.delete( c );
		s.delete( c.id.parent );
		tx.commit();
		s.close();
	}

	@Test
	public void testManyToOneInCompositeId() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Channel channel = new Channel();
		s.persist( channel );
		Presenter pres = new Presenter();
		pres.name = "Casimir";
		s.persist( pres );
		TvMagazinPk pk = new TvMagazinPk();
		TvMagazin mag = new TvMagazin();
		mag.time = new Date();
		mag.id = pk;
		pk.channel = channel;
		pk.presenter = pres;
		s.persist( mag );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		mag = (TvMagazin) s.createQuery( "from TvMagazin mag" ).uniqueResult();
		assertNotNull( mag.id );
		assertNotNull( mag.id.channel );
		assertEquals( channel.id, mag.id.channel.id );
		assertNotNull( mag.id.presenter );
		assertEquals( pres.name, mag.id.presenter.name );
		s.delete( mag );
		s.delete( mag.id.channel );
		s.delete( mag.id.presenter );
		tx.commit();
		s.close();
	}

	@Test
	public void testManyToOneInCompositeIdClass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Order order = new Order();
		s.persist( order );
		Product product = new Product();
		product.name = "small car";
		s.persist( product );
		OrderLine orderLine = new OrderLine();
		orderLine.order = order;
		orderLine.product = product;
		s.persist( orderLine );
		s.flush();
		s.clear();

		orderLine = (OrderLine) s.createQuery( "select ol from OrderLine ol" ).uniqueResult();
		assertNotNull( orderLine.order );
		assertEquals( order.id, orderLine.order.id );
		assertNotNull( orderLine.product );
		assertEquals( product.name, orderLine.product.name );

		tx.rollback();
		s.close();
	}

	@Test
	public void testSecondaryTableWithCompositeId() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Channel channel = new Channel();
		s.persist( channel );
		Presenter pres = new Presenter();
		pres.name = "Tim Russet";
		s.persist( pres );
		TvMagazinPk pk = new TvMagazinPk();
		TvProgram program = new TvProgram();
		program.time = new Date();
		program.id = pk;
		program.text = "Award Winning Programming";
		pk.channel = channel;
		pk.presenter = pres;
		s.persist( program );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		program = (TvProgram) s.createQuery( "from TvProgram pr" ).uniqueResult();
		assertNotNull( program.id );
		assertNotNull( program.id.channel );
		assertEquals( channel.id, program.id.channel.id );
		assertNotNull( program.id.presenter );
		assertNotNull( program.text );
		assertEquals( pres.name, program.id.presenter.name );
		s.delete( program );
		s.delete( program.id.channel );
		s.delete( program.id.presenter );
		tx.commit();
		s.close();
	}

	@Test
	public void testSecondaryTableWithIdClass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Channel channel = new Channel();
		s.persist( channel );
		Presenter pres = new Presenter();
		pres.name = "Bob";
		s.persist( pres );
		TvProgramIdClass program = new TvProgramIdClass();
		program.time = new Date();
		program.channel = channel;
		program.presenter = pres;
		program.text = "Jump the shark programming";
		s.persist( program );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		program = (TvProgramIdClass) s.createQuery( "from TvProgramIdClass pr" ).uniqueResult();
		assertNotNull( program.channel );
		assertEquals( channel.id, program.channel.id );
		assertNotNull( program.presenter );
		assertNotNull( program.text );
		assertEquals( pres.name, program.presenter.name );
		s.delete( program );
		s.delete( program.channel );
		s.delete( program.presenter );
		tx.commit();
		s.close();
	}

	@Test
	public void testQueryInAndComposite() {

		Session s = openSession(  );
		Transaction transaction = s.beginTransaction();
		createData( s );
        s.flush();
        List ids = new ArrayList<SomeEntityId>(2);
        ids.add( new SomeEntityId(1,12) );
        ids.add( new SomeEntityId(10,23) );

        Criteria criteria = s.createCriteria( SomeEntity.class );
        Disjunction disjunction = Restrictions.disjunction();

        disjunction.add( Restrictions.in( "id", ids  ) );
        criteria.add( disjunction );

        List list = criteria.list();
        assertEquals( 2, list.size() );
		transaction.rollback();
		s.close();
	}

	@Test
    public void testQueryInAndCompositeWithHQL() {
        Session s = openSession(  );
        Transaction transaction = s.beginTransaction();
        createData( s );
        s.flush();
        List ids = new ArrayList<SomeEntityId>(2);
        ids.add( new SomeEntityId(1,12) );
        ids.add( new SomeEntityId(10,23) );
        ids.add( new SomeEntityId(10,22) );
        Query query=s.createQuery( "from SomeEntity e where e.id in :idList" );
        query.setParameterList( "idList", ids );
        List list=query.list();
        assertEquals( 3, list.size() );
        transaction.rollback();
        s.close();
    }

	private void createData(Session s){
        SomeEntity someEntity = new SomeEntity();
        someEntity.setId( new SomeEntityId( ) );
        someEntity.getId().setId( 1 );
        someEntity.getId().setVersion( 11 );
        someEntity.setProp( "aa" );
        s.persist( someEntity );
        
        someEntity = new SomeEntity();
        someEntity.setId( new SomeEntityId( ) );
        someEntity.getId().setId( 1 );
        someEntity.getId().setVersion( 12 );
        someEntity.setProp( "bb" );
        s.persist( someEntity );
        
        someEntity = new SomeEntity();
        someEntity.setId( new SomeEntityId( ) );
        someEntity.getId().setId( 10 );
        someEntity.getId().setVersion( 21 );
        someEntity.setProp( "cc1" );
        s.persist( someEntity );
        
        someEntity = new SomeEntity();
        someEntity.setId( new SomeEntityId( ) );
        someEntity.getId().setId( 10 );
        someEntity.getId().setVersion( 22 );
        someEntity.setProp( "cc2" );
        s.persist( someEntity );
        
        someEntity = new SomeEntity();
        someEntity.setId( new SomeEntityId( ) );
        someEntity.getId().setId( 10 );
        someEntity.getId().setVersion( 23 );
        someEntity.setProp( "cc3" );
        s.persist( someEntity );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				ParentPk.class,
				Child.class,
				ChildPk.class,
				Channel.class,
				TvMagazin.class,
				TvMagazinPk.class,
				TvProgramIdClass.class,
				TvProgram.class,
				Presenter.class,
				Order.class,
				Product.class,
				OrderLine.class,
				OrderLinePk.class,
				LittleGenius.class,
				A.class,
				AId.class,
				B.class,
				C.class,
				SomeEntity.class
		};
	}
}
