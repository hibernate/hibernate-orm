/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for various JPAQL compliance issues
 *
 * @author Steve Ebersole
 */
	public class JPAQLComplianceTest extends AbstractJPATest {
	@Test
	public void testAliasNameSameAsUnqualifiedEntityName() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select item from Item item" ).list();
		s.createQuery( "select item from Item item where item.name = 'a'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIdentifierCaseSensitive() throws Exception {
		Session s = openSession( );
		// a control test (a user reported that the JPA 'case insensitivity' support
		// caused problems with the "discriminator resolution" code; unable to reproduce)...
		s.createQuery( "from MyEntity e where e.class = MySubclassEntity" );
		s.createQuery( "from MyEntity e where e.other.class = MySubclassEntity" );
		s.createQuery( "from MyEntity where other.class = MySubclassEntity" );

		s.createQuery( "select object(I) from Item i").list();
		s.close();
	}

	@Test
	public void testIdentifierCasesensitivityAndDuplicateFromElements() throws Exception {
		Session s = openSession();
		s.createQuery( "select e from MyEntity e where exists (select 1 from MyEntity e2 where e2.other.name  = 'something' and e2.other.other = e)" );
		s.close();
	}

	@Test
	public void testGeneratedSubquery() {
		Session s = openSession();
		s.createQuery( "select c FROM Item c WHERE c.parts IS EMPTY" ).list();
		s.close();
	}

	@Test
	public void testOrderByAlias() {
		Session s = openSession();
		s.createQuery( "select c.name as myname FROM Item c ORDER BY myname" ).list();
		s.createQuery( "select p.name as name, p.stockNumber as stockNo, p.unitPrice as uPrice FROM Part p ORDER BY name, abs( p.unitPrice ), stockNo" ).list();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParameterCollectionPositional() {
		Session s = openSession();
		Query q = s.createQuery( "select item from Item item where item.id in (?1) and item.name = ?2" );
		List params = new ArrayList();
		params.add( Long.valueOf( 0 ) );
		params.add( Long.valueOf( 1 ) );
		q.setParameter( 1, params );
		q.setParameter( 2, "name" );
		q.list();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParametersMixturePositionalAndNamed() {
		Session s = openSession();
		try {
			s.createQuery( "select item from Item item where item.id = ?1 and item.name = :name" ).list();
			fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
		} catch ( IllegalArgumentException e ) {
			assertNotNull( e.getCause() );
			assertTyping( QuerySyntaxException.class, e.getCause() );
		} finally {
			s.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParametersMixtureNamedAndPositional() {
		Session s = openSession();
		try {
			s.createQuery( "select item from Item item where item.id = :id and item.name = ?1" ).list();
			fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
		} catch ( IllegalArgumentException e ) {
			assertNotNull( e.getCause() );
			assertTyping( QuerySyntaxException.class, e.getCause() );
		} finally {
			s.close();
		}
	}

	/**
	 * Positional collection parameter is expanded to the list of named parameters. In spite of this fact, initial query
	 * query is wrong in terms of JPA and exception must be thrown
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParametersMixtureNamedCollectionAndPositional() {
		Session s = openSession();
		try {
			Query q = s.createQuery( "select item from Item item where item.id in (?1) and item.name = :name" );
			List<Long> params = new ArrayList();
			params.add( Long.valueOf( 0 ) );
			params.add( Long.valueOf( 1 ) );
			q.setParameter( 1, params );
			q.setParameter( "name", "name" );
			q.list();
			fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
		}
		catch (IllegalArgumentException e) {
			assertNotNull( e.getCause() );
			assertTyping( QuerySyntaxException.class, e.getCause() );
		}
		finally {
			s.close();
		}
	}
}
