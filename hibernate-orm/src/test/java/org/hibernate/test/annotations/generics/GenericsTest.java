/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.generics;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class GenericsTest extends BaseCoreFunctionalTestCase {
	
	@SkipForDialect(value = AbstractHANADialect.class, comment = "known bug in HANA: rs.next() returns false for org.hibernate.id.enhanced.SequenceStructure$1.getNextValue() for this test")
	@Test
	public void testManyToOneGenerics() throws Exception {
		Paper white = new Paper();
		white.setName( "WhiteA4" );
		PaperType type = new PaperType();
		type.setName( "A4" );
		SomeGuy me = new SomeGuy();
		white.setType( type );
		white.setOwner( me );
		Price price = new Price();
		price.setAmount( new Double( 1 ) );
		price.setCurrency( "Euro" );
		white.setValue( price );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( type );
		s.persist( price );
		s.persist( me );
		s.persist( white );
		tx.commit();
		//s.close();
		s = openSession();
		tx = s.beginTransaction();
		white = (Paper) s.get( Paper.class, white.getId() );
		s.delete( white.getType() );
		s.delete( white.getOwner() );
		s.delete( white.getValue() );
		s.delete( white );
		tx.commit();
		//s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.AUTO_CLOSE_SESSION, "true" );
		super.configure( cfg );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Paper.class,
				PaperType.class,
				SomeGuy.class,
				Price.class,
				WildEntity.class,

				//test at deployment only test unbound property when default field access is used
				Dummy.class
		};
	}
}
