/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.namingstrategy.components;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultComponentSafeNamingStrategy;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class ComponentNamingStrategyTest extends BaseUnitTestCase {
	@Test
	public void testDefaultNamingStrategy() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Container.class ).addAnnotatedClass( Item.class );
		cfg.buildMappings();
		PersistentClass pc = cfg.getClassMapping( Container.class.getName() );
		Property p = pc.getProperty( "items" );
		Bag value = assertTyping( Bag.class, p.getValue() );
		SimpleValue elementValue = assertTyping( SimpleValue.class, value.getElement() );
		assertEquals( 1, elementValue.getColumnSpan() );
		Column column = assertTyping( Column.class, elementValue.getColumnIterator().next() );
		assertFalse( column.getName().contains( "&&" ) );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-6005" )
	public void testComponentSafeNamingStrategy() {
		Configuration cfg = new Configuration();
		cfg.setNamingStrategy( DefaultComponentSafeNamingStrategy.INSTANCE );
		cfg.addAnnotatedClass( Container.class ).addAnnotatedClass( Item.class );
		cfg.buildMappings();
		PersistentClass pc = cfg.getClassMapping( Container.class.getName() );
		Property p = pc.getProperty( "items" );
		Bag value = assertTyping( Bag.class, p.getValue() );
		SimpleValue elementValue = assertTyping(  SimpleValue.class, value.getElement() );
		assertEquals( 1, elementValue.getColumnSpan() );
		Column column = assertTyping( Column.class, elementValue.getColumnIterator().next() );
		assertFalse( column.getName().contains( "&&" ) );
	}
}
