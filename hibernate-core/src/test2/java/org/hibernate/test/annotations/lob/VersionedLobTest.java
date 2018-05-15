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
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class VersionedLobTest extends AbstractLobTest<VersionedBook, VersionedCompiledCode> {
	@Override
	protected Class<VersionedBook> getBookClass() {
		return VersionedBook.class;
	}

	@Override
	protected Integer getId(VersionedBook book) {
		return book.getId();
	}

	@Override
	protected Class<VersionedCompiledCode> getCompiledCodeClass() {
		return VersionedCompiledCode.class;
	}

	@Override
	protected Integer getId(VersionedCompiledCode compiledCode) {
		return compiledCode.getId();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				VersionedBook.class,
				VersionedCompiledCode.class
		};
	}

	@Test
	public void testVersionUnchangedPrimitiveCharArray() throws Exception {
		VersionedBook book = createBook();
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
		VersionedBook loadedBook = getBookClass().cast( s.get( getBookClass(), getId( book ) ) );
		assertEquals( loadedBook.getVersion(), Integer.valueOf( 0 ) );
		s.flush();
		assertEquals( loadedBook.getVersion(), Integer.valueOf( 0 ) );
		s.delete( loadedBook );
		tx.commit();
		s.close();

	}

	@Test
	public void testVersionUnchangedCharArray() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		VersionedBook b = createBook();
		b.setShortDescription( "Hibernate Bible" );
		b.setCode( new Character[] { 'a', 'b', 'c' } );
		s.persist( b );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		VersionedBook b2 = getBookClass().cast( s.get( getBookClass(), getId( b ) ) );
		assertNotNull( b2 );
		assertEquals( b2.getCode()[1].charValue(), b.getCode()[1].charValue() );
		assertEquals( b2.getVersion(), Integer.valueOf( 0 ) );
		s.flush();
		assertEquals( b2.getVersion(), Integer.valueOf( 0 ) );
		s.delete( b2 );
		tx.commit();
		s.close();
	}

	@Test
	public void testVersionUnchangedString() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		VersionedBook b = createBook();
		b.setShortDescription( "Hibernate Bible" );
		b.setFullText( "Hibernate in Action aims to..." );
		s.persist( b );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		VersionedBook b2 = getBookClass().cast( s.get( getBookClass(), getId( b ) ) );
		assertNotNull( b2 );
		assertEquals( b2.getFullText(), b.getFullText() );
		assertEquals( b2.getVersion(), Integer.valueOf( 0 ) );
		s.flush();
		assertEquals( b2.getVersion(), Integer.valueOf( 0 ) );
		s.delete( b2 );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5811")
	public void testVersionUnchangedByteArray() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		VersionedCompiledCode cc = createCompiledCode();
		Byte[] header = new Byte[2];
		header[0] = new Byte( ( byte ) 3 );
		header[1] = new Byte( ( byte ) 0 );
		cc.setHeader( header );
		s.persist( cc );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		VersionedCompiledCode recompiled = getCompiledCodeClass().cast( s.get( getCompiledCodeClass(), getId( cc ) ) );
		assertEquals( recompiled.getHeader()[1], cc.getHeader()[1] );
		assertEquals( recompiled.getVersion(), Integer.valueOf( 0 ) );
		s.flush();
		assertEquals( recompiled.getVersion(), Integer.valueOf( 0 ) );
		s.delete( recompiled );
		tx.commit();
		s.close();
	}

	@Test
	public void testVersionUnchangedPrimitiveByteArray() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		VersionedCompiledCode cc = createCompiledCode();
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
		VersionedCompiledCode recompiled = getCompiledCodeClass().cast( s.get( getCompiledCodeClass(), getId( cc ) ) );
		assertEquals( recompiled.getFullCode()[codeSize - 1], cc.getFullCode()[codeSize - 1] );
		assertEquals( recompiled.getVersion(), Integer.valueOf( 0 ) );
		s.flush();
		assertEquals( recompiled.getVersion(), Integer.valueOf( 0 ) );
		s.delete( recompiled );
		tx.commit();
		s.close();
	}
}
