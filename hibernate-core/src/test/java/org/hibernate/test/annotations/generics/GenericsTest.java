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
package org.hibernate.test.annotations.generics;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel( jiraKey = "HHH-9049" )
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
				Stuff.class,
				PricedStuff.class,
				Item.class,
				PaperType.class,
				SomeGuy.class,
				Price.class,
				WildEntity.class,

				//test at deployment only test unbound property when default field access is used
				Dummy.class
		};
	}
}
