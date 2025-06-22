/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositeelement;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
public class CompositeElementTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "org/hibernate/orm/test/compositeelement/Parent.hbm.xml" };
	}

	@Override
	protected void afterMetadataBuilt(Metadata metadata) {
		Collection children = metadata.getCollectionBinding( Parent.class.getName() + ".children" );
		Component childComponents = ( Component ) children.getElement();
		Formula f = ( Formula ) childComponents.getProperty( "bioLength" ).getValue().getSelectables().get( 0 );

//		SQLFunction lengthFunction = metadata.getDatabase().getJdbcEnvironment().getDialect().getFunctions().get( "length" );
//		if ( lengthFunction != null ) {
//			ArrayList args = new ArrayList();
//			args.add( "bio" );
//			f.setFormula( lengthFunction.render( StandardBasicTypes.INTEGER, args, null ) );
//		}
	}

	@Test
	public void testHandSQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child( "Child One" );
		Parent p = new Parent( "Parent" );
		p.getChildren().add( c );
		c.setParent( p );
		s.persist( p );
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
		s.remove( p );
		t.commit();
		s.close();
	}

	@Test
	public void testCustomColumnReadAndWrite() {
		inTransaction( s -> {
			Child c = new Child( "Child One" );
			c.setPosition( 1 );
			Parent p = new Parent( "Parent" );
			p.getChildren().add( c );
			c.setParent( p );
			s.persist( p );
			s.flush();

			// Oracle returns BigDecimaal while other dialects return Integer;
			// casting to Number so it works on all dialects
			Number sqlValue = ( (Number) s.createNativeQuery(
					"select child_position from ParentChild c where c.name='Child One'" )
					.uniqueResult() );
			assertEquals( 0, sqlValue.intValue() );

			Integer hqlValue = (Integer) s.createQuery(
					"select c.position from Parent p join p.children c where p.name='Parent'" )
					.uniqueResult();
			assertEquals( 1, hqlValue.intValue() );

//			p = (Parent) s.createCriteria( Parent.class ).add( Restrictions.eq( "name", "Parent" ) ).uniqueResult();

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Parent> criteria = criteriaBuilder.createQuery( Parent.class );
			Root<Parent> root = criteria.from( Parent.class );
			criteria.where( criteriaBuilder.equal( root.get( "name" ),"Parent"  ) );

			p = s.createQuery( criteria ).uniqueResult();

			c = (Child) p.getChildren().iterator().next();
			assertEquals( 1, c.getPosition() );

			p = s.createQuery( "from Parent p join p.children c where c.position = 1", Parent.class ).uniqueResult();
			c = (Child) p.getChildren().iterator().next();
			assertEquals( 1, c.getPosition() );

			c.setPosition( 2 );
			s.flush();
			sqlValue = ( (Number) s.createNativeQuery(
					"select child_position from ParentChild c where c.name='Child One'" )
					.uniqueResult() );
			assertEquals( 1, sqlValue.intValue() );
			s.remove( p );
		} );
	}

}
