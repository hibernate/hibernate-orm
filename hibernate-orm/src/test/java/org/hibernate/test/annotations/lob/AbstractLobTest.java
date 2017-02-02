/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public abstract class AbstractLobTest<B extends AbstractBook, C extends AbstractCompiledCode>
		extends BaseCoreFunctionalTestCase {

	protected abstract Class<B> getBookClass();

	protected B createBook() {
		try {
			return getBookClass().newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( "Could not create an instance of type " + getBookClass().getName(), ex );
		}
	}

	protected abstract Integer getId(B book);

	protected abstract Class<C> getCompiledCodeClass();

	protected C createCompiledCode() {
		try {
			return getCompiledCodeClass().newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( "Could not create an instance of type " + getCompiledCodeClass().getName(), ex );
		}
	}

	protected abstract Integer getId(C compiledCode);

	@Test
	public void testSerializableToBlob() throws Exception {
		B book = createBook();
		Editor editor = new Editor();
		editor.setName( "O'Reilly" );
		book.setEditor( editor );
		book.setCode2( new char[] { 'r' } );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( book );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		B loadedBook = getBookClass().cast( s.get( getBookClass(), getId( book ) ) );
		assertNotNull( loadedBook.getEditor() );
		assertEquals( book.getEditor().getName(), loadedBook.getEditor().getName() );
		loadedBook.setEditor( null );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		loadedBook = getBookClass().cast( s.get( getBookClass(), getId( book ) ) );
		assertNull( loadedBook.getEditor() );
		tx.commit();
		s.close();

	}

	@Test
	public void testClob() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		B b = createBook();
		b.setShortDescription( "Hibernate Bible" );
		b.setFullText( "Hibernate in Action aims to..." );
		b.setCode( new Character[] { 'a', 'b', 'c' } );
		b.setCode2( new char[] { 'a', 'b', 'c' } );
		s.persist( b );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		B b2 = getBookClass().cast( s.get( getBookClass(), getId( b ) ) );
		assertNotNull( b2 );
		assertEquals( b2.getFullText(), b.getFullText() );
		assertEquals( b2.getCode()[1].charValue(), b.getCode()[1].charValue() );
		assertEquals( b2.getCode2()[2], b.getCode2()[2] );
		tx.commit();
		s.close();
	}

	@Test
	public void testBlob() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		C cc = createCompiledCode();
		Byte[] header = new Byte[2];
		header[0] = new Byte( ( byte ) 3 );
		header[1] = new Byte( ( byte ) 0 );
		cc.setHeader( header );
		int codeSize = 5;
		byte[] full = new byte[codeSize];
		for ( int i = 0; i < codeSize; i++ ) {
			full[i] = ( byte ) ( 1 + i );
		}
		cc.setFullCode( full );
		s.persist( cc );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		C recompiled = getCompiledCodeClass().cast( s.get( getCompiledCodeClass(), getId( cc ) ) );
		assertEquals( recompiled.getHeader()[1], cc.getHeader()[1] );
		assertEquals( recompiled.getFullCode()[codeSize - 1], cc.getFullCode()[codeSize - 1] );
		tx.commit();
		s.close();
	}

	@Test
	public void testBinary() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		C cc = createCompiledCode();
		byte[] metadata = new byte[2];
		metadata[0] = ( byte ) 3;
		metadata[1] = ( byte ) 0;
		cc.setMetadata( metadata );
		s.persist( cc );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		C recompiled = getCompiledCodeClass().cast( s.get( getCompiledCodeClass(), getId( cc ) ) );
		assertEquals( recompiled.getMetadata()[1], cc.getMetadata()[1] );
		tx.commit();
		s.close();
	}
}
