/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.query.Query;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

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
			List<Long> params = new ArrayList<>();
			params.add( 0L );
			params.add( 1L );
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

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParameterCollectionParenthesesAndPositional() {
		final Item item = new Item( "Mouse" );
		item.setId( 1L );
		final Item item2 = new Item( "Computer" );
		item2.setId( 2L );

		Session s = openSession();
		try {
			s.getTransaction().begin();
			s.save( item );
			s.save( item2 );
			s.getTransaction().commit();

			s.getTransaction().begin();
			Query q = s.createQuery( "select item from Item item where item.id in(?1) and item.name in (?2) and item.id in(?1)" );

			List<Long> idParams = new ArrayList<>();
			idParams.add( item.getId() );
			idParams.add( item2.getId() );
			q.setParameter( 1, idParams );

			List<String> nameParams = new ArrayList<>();
			nameParams.add( item.getName() );
			nameParams.add( item2.getName() );
			q.setParameter( 2, nameParams );

			List result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
		}
		catch (Exception e){
			if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}
}
