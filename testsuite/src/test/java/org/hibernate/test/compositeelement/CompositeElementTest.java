//$Id: CompositeElementTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.compositeelement;

import java.util.ArrayList;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Mappings;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;

/**
 * @author Gavin King
 */
public class CompositeElementTest extends FunctionalTestCase {

	public CompositeElementTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "compositeelement/Parent.hbm.xml" };
	}

	public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
		super.afterConfigurationBuilt( mappings, dialect );
		Collection children = mappings.getCollection( Parent.class.getName() + ".children" );
		Component childComponents = ( Component ) children.getElement();
		Formula f = ( Formula ) childComponents.getProperty( "bioLength" ).getValue().getColumnIterator().next();

		SQLFunction lengthFunction = ( SQLFunction ) dialect.getFunctions().get( "length" );
		if ( lengthFunction != null ) {
			ArrayList args = new ArrayList();
			args.add( "bio" );
			f.setFormula( lengthFunction.render( Hibernate.INTEGER, args, null ) );
		}
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CompositeElementTest.class );
	}

	public void testHandSQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child( "Child One" );
		Parent p = new Parent( "Parent" );
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		s.flush();

		p.getChildren().remove( c );
		c.setParent( null );
		s.flush();

		p.getChildren().add( c );
		c.setParent( p );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "select distinct p from Parent p join p.children c where c.name like 'Child%'" ).uniqueResult();
		s.clear();
		s.createQuery( "select new Child(c.name) from Parent p left outer join p.children c where c.name like 'Child%'" )
				.uniqueResult();
		s.clear();
		//s.createQuery("select c from Parent p left outer join p.children c where c.name like 'Child%'").uniqueResult(); //we really need to be able to do this!
		s.clear();
		p = ( Parent ) s.createQuery( "from Parent p left join fetch p.children" ).uniqueResult();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( p );
		t.commit();
		s.close();
	}
	
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child( "Child One" );
		c.setPosition( 1 );
		Parent p = new Parent( "Parent" );
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		s.flush();

		Integer sqlValue = (Integer) s.createSQLQuery("select child_position from parentchild c where c.name='Child One'")
				.uniqueResult();
		assertEquals( 0, sqlValue.intValue() );

		Integer hqlValue = (Integer)s.createQuery("select c.position from Parent p join p.children c where p.name='Parent'")
				.uniqueResult();
		assertEquals( 1, hqlValue.intValue() );

		p = (Parent)s.createCriteria(Parent.class).add(Restrictions.eq("name", "Parent")).uniqueResult();
		c = (Child)p.getChildren().iterator().next();
		assertEquals( 1, c.getPosition() );

		p = (Parent)s.createQuery("from Parent p join p.children c where c.position = 1").uniqueResult();
		c = (Child)p.getChildren().iterator().next();
		assertEquals( 1, c.getPosition() );

		c.setPosition( 2 );
		s.flush();
		sqlValue = (Integer) s.createSQLQuery("select child_position from parentchild c where c.name='Child One'")
				.uniqueResult();
		assertEquals( 1, sqlValue.intValue() );
		s.delete( p );
		t.commit();
		s.close();
	}

}

