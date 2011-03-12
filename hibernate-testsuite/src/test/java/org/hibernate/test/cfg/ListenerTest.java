/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cfg;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * {@inheritDoc}
 *
 * @author Gail Badner
 */
public class ListenerTest extends UnitTestCase {

	public static class InvalidListenerForTest {
	}

	public static class DeleteListenerForTest implements DeleteEventListener {
		public void onDelete(DeleteEvent event) throws HibernateException {
		}

		public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException {
		}
	}

	public static class AnotherDeleteListenerForTest implements DeleteEventListener {
		public void onDelete(DeleteEvent event) throws HibernateException {
		}

		public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException {
		}
	}

	public ListenerTest(String string) {
		super( string );
	}

	public void testSetListenerNullClass() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListener( "delete", null );
		assertEquals( 0, cfg.getEventListeners().getDeleteEventListeners().length );
	}

	public void testSetListenersNullClass() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners( "delete", null );
		assertEquals( 0, cfg.getEventListeners().getDeleteEventListeners().length );
	}

	public void testSetListenerEmptyClassNameArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", new String[] { } );
			fail( "should have thrown java.lang.ArrayStoreException" );
		}
		catch ( ArrayStoreException ex ) {
			// expected
		}
	}

	public void testSetListenersEmptyClassNsmeArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners( "delete", new String[] { } );
		assertEquals( 0, cfg.getEventListeners().getDeleteEventListeners().length );
	}

	public void testSetListenerEmptyClassObjectArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", new Object[] { } );
			fail( "should have thrown java.lang.ArrayStoreException" );
		}
		catch ( ArrayStoreException ex ) {
			// expected
		}
	}

	public void testSetListenersEmptyClassObjectArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( "delete", new Object[] { } );
			fail( "should have thrown ClassCastException" );
		}
		catch ( ClassCastException ex ) {
			// expected
		}
	}

	public void testSetListenerEmptyClassArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", new DefaultDeleteEventListener[] { } );
			fail( "should have thrown java.lang.ArrayStoreException" );
		}
		catch ( ArrayStoreException ex ) {
			// expected
		}
	}

	public void testSetListenersEmptyClassArray() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners( "delete", new DefaultDeleteEventListener[] { } );
		assertEquals( 0, cfg.getEventListeners().getDeleteEventListeners().length );
	}

	public void testSetListenerUnknownClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", "UnknownClassName" );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenersUnknownClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( "delete", new String[] { "UnknownClassName" } );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenerInvalidClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", InvalidListenerForTest.class.getName() );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenersInvalidClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( "delete", new String[] { InvalidListenerForTest.class.getName() } );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenerClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListener( "delete", DeleteListenerForTest.class.getName() );
		assertEquals( 1, cfg.getEventListeners().getDeleteEventListeners().length );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[0] instanceof DeleteListenerForTest );
	}

	public void testSetListenersClassName() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners( "delete", new String[] { DeleteListenerForTest.class.getName() } );
		assertEquals( 1, cfg.getEventListeners().getDeleteEventListeners().length );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[0] instanceof DeleteListenerForTest );
	}

	public void testSetListenerClassNames() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener(
					"delete", new String[] {
					DeleteListenerForTest.class.getName(),
					AnotherDeleteListenerForTest.class.getName()
			}
			);
			fail( "should have thrown java.lang.ArrayStoreException" );
		}
		catch ( ArrayStoreException ex ) {
			// expected
		}
	}

	public void testSetListenersClassNames() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners(
				"delete", new String[] {
				DeleteListenerForTest.class.getName(),
				AnotherDeleteListenerForTest.class.getName()
		}
		);
		assertEquals( 2, cfg.getEventListeners().getDeleteEventListeners().length );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[0] instanceof DeleteListenerForTest );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[1] instanceof AnotherDeleteListenerForTest );
	}

	public void testSetListenerClassInstance() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListener( "delete", new DeleteListenerForTest() );
		assertEquals( 1, cfg.getEventListeners().getDeleteEventListeners().length );
	}

	public void testSetListenersClassInstances() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		cfg.setListeners(
				"delete", new DeleteEventListener[] {
				new DeleteListenerForTest(),
				new AnotherDeleteListenerForTest()
		}
		);
		assertEquals( 2, cfg.getEventListeners().getDeleteEventListeners().length );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[0] instanceof DeleteListenerForTest );
		assertTrue( cfg.getEventListeners().getDeleteEventListeners()[1] instanceof AnotherDeleteListenerForTest );
	}

	public void testSetListenerInvalidClassInstance() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "delete", new InvalidListenerForTest() );
			fail( "should have thrown java.lang.ArrayStoreException" );
		}
		catch ( ArrayStoreException ex ) {
			// expected
		}
	}

	public void testSetListenersInvalidClassInstances() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( "delete", new InvalidListenerForTest[] { new InvalidListenerForTest() } );
			fail( "should have thrown java.lang.ClassCastException" );
		}
		catch ( ClassCastException ex ) {
			// expected
		}
	}

	public void testSetListenerNullType() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( null, new DeleteListenerForTest() );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenersNullType() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( null, new DeleteEventListener[] { new DeleteListenerForTest() } );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenerUnknownType() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListener( "unknown-type", new DeleteListenerForTest() );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	public void testSetListenersUnknownType() {
		Configuration cfg = new Configuration();
		assertNotNull( cfg.getEventListeners().getDeleteEventListeners() );
		try {
			cfg.setListeners( "unknown-type", new DeleteEventListener[] { new DeleteListenerForTest() } );
			fail( "should have thrown MappingException" );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}
}
