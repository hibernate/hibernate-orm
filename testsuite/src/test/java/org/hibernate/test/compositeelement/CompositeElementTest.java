//$Id: CompositeElementTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.compositeelement;

import java.util.ArrayList;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Mappings;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
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
			f.setFormula( lengthFunction.render( args, null ) );
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
		final double HEIGHT_INCHES = 49;
		final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child( "Child One" );
		c.setHeightInches(HEIGHT_INCHES);
		Parent p = new Parent( "Parent" );
		p.getChildren().add( c );
		c.setParent( p );
		s.save( p );
		s.flush();
		
		// Test value conversion during insert		
		Double heightViaSql = (Double)s.createSQLQuery("select height_centimeters from parentchild c where c.name='Child One'")
			.uniqueResult();
		assertEquals(HEIGHT_CENTIMETERS, heightViaSql, 0.01d);
		
		// Test projection		
		Double heightViaHql = (Double)s.createQuery("select c.heightInches from Parent p join p.children c where p.name='Parent'")
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);
		
		// Test entity load via criteria
		p = (Parent)s.createCriteria(Parent.class).add(Restrictions.eq("name", "Parent")).uniqueResult();
		c = (Child)p.getChildren().iterator().next();
		assertEquals(HEIGHT_INCHES, c.getHeightInches(), 0.01d);
		
		// Test predicate and entity load via HQL
		p = (Parent)s.createQuery("from Parent p join p.children c where c.heightInches between ? and ?")
			.setDouble(0, HEIGHT_INCHES - 0.01d)
			.setDouble(1, HEIGHT_INCHES + 0.01d)
			.uniqueResult();
		c = (Child)p.getChildren().iterator().next();
		assertEquals(HEIGHT_INCHES, c.getHeightInches(), 0.01d);
		
		// Test update
		c.setHeightInches(1);
		s.flush();
		heightViaSql = (Double)s.createSQLQuery("select height_centimeters from parentchild c where c.name='Child One'").uniqueResult();
		assertEquals(2.54d, heightViaSql, 0.01d);
		s.delete( p );
		t.commit();
		s.close();
	}

}

