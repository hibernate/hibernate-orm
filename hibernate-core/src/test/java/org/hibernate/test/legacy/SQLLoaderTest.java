/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: SQLLoaderTest.java 11383 2007-04-02 15:34:02Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.TimesTenDialect;

import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SQLLoaderTest extends LegacyTestCase {
	static int nextInt = 1;
	static long nextLong = 1;

	@Override
	public String[] getMappings() {
		return new String[] {
			"legacy/ABC.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/SingleSeveral.hbm.xml",
			"legacy/Componentizable.hbm.xml",
            "legacy/CompositeIdId.hbm.xml"
		};
	}

	@Test
	public void testTS() throws Exception {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Simple sim = new Simple( Long.valueOf(1) );
		sim.setDate( new Date() );
		session.save( sim );
		Query q = session.createSQLQuery( "select {sim.*} from Simple {sim} where {sim}.date_ = ?" ).addEntity( "sim", Simple.class );
		q.setTimestamp( 0, sim.getDate() );
		assertTrue ( q.list().size()==1 );
		session.delete(sim);
		txn.commit();
		session.close();
	}

	@Test
	public void testFindBySQLStar() throws HibernateException, SQLException {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from Assignable" ).list() ) {
			session.delete( entity );
		}
		for ( Object entity : session.createQuery( "from Category" ).list() ) {
			session.delete( entity );
		}
		for ( Object entity : session.createQuery( "from Simple" ).list() ) {
			session.delete( entity );
		}
		for ( Object entity : session.createQuery( "from A" ).list() ) {
			session.delete( entity );
		}

		Category s = new Category();
		s.setName(String.valueOf(nextLong++));
		session.save(s);

		Simple simple = new Simple( Long.valueOf(nextLong++) );
		simple.init();
		session.save( simple );

		A a = new A();
		session.save(a);

		B b = new B();
		session.save(b);
		session.flush();

		session.createSQLQuery( "select {category.*} from category {category}" ).addEntity( "category", Category.class ).list();
		session.createSQLQuery( "select {simple.*} from Simple {simple}" ).addEntity( "simple", Simple.class ).list();
		session.createSQLQuery( "select {a.*} from TA {a}" ).addEntity( "a", A.class ).list();

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testFindBySQLProperties() throws HibernateException, SQLException {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from Category" ).list() ) {
			session.delete( entity );
		}

		Category s = new Category();
		s.setName(String.valueOf(nextLong++));
		session.save(s);

		s = new Category();
		s.setName("WannaBeFound");
		session.flush();

		Query query = session.createSQLQuery( "select {category.*} from category {category} where {category}.name = :name" )
				.addEntity( "category", Category.class );

		query.setProperties(s);
		//query.setParameter("name", s.getName());

		query.list();

		query = session.createSQLQuery( "select {category.*} from category {category} where {category}.name in (:names)" )
				.addEntity( "category", Category.class );
		String[] str = new String[] { "WannaBeFound", "NotThere" };
		query.setParameterList("names", str);
		query.uniqueResult();

		query = session.createSQLQuery( "select {category.*} from category {category} where {category}.name in :names" )
				.addEntity( "category", Category.class );
		query.setParameterList("names", str);
		query.uniqueResult();

		query = session.createSQLQuery( "select {category.*} from category {category} where {category}.name in (:names)" )
				.addEntity( "category", Category.class );
		str = new String[] { "WannaBeFound" };
		query.setParameterList("names", str);
		query.uniqueResult();

		query = session.createSQLQuery( "select {category.*} from category {category} where {category}.name in :names" )
				.addEntity( "category", Category.class );
		query.setParameterList("names", str);
		query.uniqueResult();

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testFindBySQLAssociatedObjects() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		for ( Object entity : s.createQuery( "from Assignable" ).list() ) {
			s.delete( entity );
		}
		for ( Object entity : s.createQuery( "from Category" ).list() ) {
			s.delete( entity );
		}

		Category c = new Category();
		c.setName("NAME");
		Assignable assn = new Assignable();
		assn.setId("i.d.");
		List l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List list = s.createSQLQuery( "select {category.*} from category {category}" ).addEntity( "category", Category.class ).list();
		list.get(0);
		s.getTransaction().commit();
		s.close();
		
		if ( getDialect() instanceof MySQLDialect ) {
			return;
		}

		s = openSession();
		s.beginTransaction();

		Query query = s.getNamedQuery("namedsql");
		assertNotNull(query);
		list = query.list();
        assertNotNull(list);
		
		Object[] values = (Object[]) list.get(0);
		assertNotNull(values[0]);
		assertNotNull(values[1]);
		assertTrue("wrong type: " + values[0].getClass(), values[0] instanceof Category);
		assertTrue("wrong type: " + values[1].getClass(), values[1] instanceof Assignable);

		s.getTransaction().commit();
		s.close();

	}

	@Test
	@SkipForDialect( MySQLDialect.class )
	public void testPropertyResultSQL() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		for ( Object entity : s.createQuery( "from Assignable" ).list() ) {
			s.delete( entity );
		}
		for ( Object entity : s.createQuery( "from Category" ).list() ) {
			s.delete( entity );
		}

		Category c = new Category();
		c.setName("NAME");
		Assignable assn = new Assignable();
		assn.setId("i.d.");
		List l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Query query = s.getNamedQuery("nonaliasedsql");
		assertNotNull(query);
		List list = query.list();
        assertNotNull(list);
		assertTrue(list.get(0) instanceof Category);
		s.getTransaction().commit();
		s.close();

	}

	@Test
	public void testFindBySQLMultipleObject() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		for ( Object entity : s.createQuery( "from Assignable" ).list() ) {
			s.delete( entity );
		}
		for ( Object entity : s.createQuery( "from Category" ).list() ) {
			s.delete( entity );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Category c = new Category();
		c.setName("NAME");
		Assignable assn = new Assignable();
		assn.setId("i.d.");
		List l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.flush();
		c = new Category();
		c.setName("NAME2");
		assn = new Assignable();
		assn.setId("i.d.2");
		l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.flush();

		assn = new Assignable();
		assn.setId("i.d.3");
		s.save(assn);
		s.getTransaction().commit();
		s.close();

		if ( getDialect() instanceof MySQLDialect ) {
			return;
		}

		s = openSession();
		s.beginTransaction();
		String sql = "select {category.*}, {assignable.*} from category {category}, \"assign-able\" {assignable}";

		List list = s.createSQLQuery( sql ).addEntity( "category", Category.class ).addEntity( "assignable", Assignable.class ).list();

		assertTrue(list.size() == 6); // crossproduct of 2 categories x 3 assignables
		assertTrue(list.get(0) instanceof Object[]);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFindBySQLParameters() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		for ( Object entity : s.createQuery( "from Assignable" ).list() ) {
			s.delete( entity );
		}
		for ( Object entity : s.createQuery( "from Category" ).list() ) {
			s.delete( entity );
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Category c = new Category();
		c.setName("Good");
		Assignable assn = new Assignable();
		assn.setId("i.d.");
		List l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.flush();
		c = new Category();
		c.setName("Best");
		assn = new Assignable();
		assn.setId("i.d.2");
		l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.flush();
		c = new Category();
		c.setName("Better");
		assn = new Assignable();
		assn.setId("i.d.7");
		l = new ArrayList();
		l.add(c);
		assn.setCategories(l);
		c.setAssignable(assn);
		s.save(assn);
		s.flush();

		assn = new Assignable();
		assn.setId("i.d.3");
		s.save(assn);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Query basicParam = s.createSQLQuery( "select {category.*} from category {category} where {category}.name = 'Best'" )
				.addEntity( "category", Category.class );
		List list = basicParam.list();
		assertEquals(1, list.size());

		Query unnamedParam = s.createSQLQuery( "select {category.*} from category {category} where {category}.name = ? or {category}.name = ?" )
				.addEntity( "category", Category.class );
		unnamedParam.setString(0, "Good");
		unnamedParam.setString(1, "Best");
		list = unnamedParam.list();
		assertEquals(2, list.size());

		Query namedParam = s.createSQLQuery( "select {category.*} from category {category} where ({category}.name=:firstCat or {category}.name=:secondCat)" )
				.addEntity( "category", Category.class);
		namedParam.setString("firstCat", "Better");
		namedParam.setString("secondCat", "Best");
		list = namedParam.list();
		assertEquals(2, list.size());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SkipForDialect( { HSQLDialect.class, PostgreSQL81Dialect.class, PostgreSQLDialect.class } )
	public void testEscapedJDBC() throws HibernateException, SQLException {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from A" ).list() ) {
			session.delete( entity );
		}
		A savedA = new A();
		savedA.setName("Max");
		session.save(savedA);

		B savedB = new B();
		session.save(savedB);
		session.flush();

		int count = session.createQuery("from A").list().size();
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		Query query;
		if( getDialect() instanceof TimesTenDialect) {
            // TimesTen does not permit general expressions (like UPPER) in the second part of a LIKE expression,
            // so we execute a similar test 
            query = session.createSQLQuery("select identifier_column as {a.id}, clazz_discriminata as {a.class}, count_ as {a.count}, name as {a.name} from TA where {fn ucase(name)} like 'MAX'" )
					.addEntity( "a", A.class );
        }
		else {
            query = session.createSQLQuery( "select identifier_column as {a.id}, clazz_discriminata as {a.class}, count_ as {a.count}, name as {a.name} from TA where {fn ucase(name)} like {fn ucase('max')}" )
					.addEntity( "a", A.class );
        }
		List list = query.list();

		assertNotNull(list);
		assertEquals(1, list.size());
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testDoubleAliasing() throws HibernateException, SQLException {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from A" ).list() ) {
			session.delete( entity );
		}
		A savedA = new A();
		savedA.setName("Max");
		session.save(savedA);

		B savedB = new B();
		session.save(savedB);
		session.flush();

		int count = session.createQuery("from A").list().size();
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		String sql = "select a.identifier_column as {a1.id}, " +
				"    a.clazz_discriminata as {a1.class}, " +
				"    a.count_ as {a1.count}, " +
				"    a.name as {a1.name}, " +
				"    b.identifier_column as {a2.id}, " +
				"    b.clazz_discriminata as {a2.class}, " +
				"    b.count_ as {a2.count}, " +
				"    b.name as {a2.name} " +
				"from TA a, TA b " +
				"where a.identifier_column = b.identifier_column";
		Query query = session.createSQLQuery( sql ).addEntity( "a1", A.class ).addEntity( "a2", A.class );
		List list = query.list();

		assertNotNull(list);
		assertEquals(2, list.size());
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testEmbeddedCompositeProperties() throws HibernateException, SQLException {
		Session session = openSession();
		session.beginTransaction();
		Single s = new Single();
		s.setId("my id");
		s.setString("string 1");
		session.save(s);
		session.getTransaction().commit();

		session = openSession();
		session.beginTransaction();

		SQLQuery query = session.createSQLQuery( "select {sing.*} from Single {sing}" ).addEntity( "sing", Single.class );
		List list = query.list();
		assertTrue(list.size()==1);

		session.clear();

		query = session.createSQLQuery( "select {sing.*} from Single {sing} where sing.id = ?" ).addEntity( "sing", Single.class );
	   	query.setString(0, "my id");
	   	list = query.list();
		assertTrue(list.size()==1);

		session.clear();

		query = session.createSQLQuery( "select s.id as {sing.id}, s.string_ as {sing.string}, s.prop as {sing.prop} from Single s where s.id = ?" )
			   .addEntity( "sing", Single.class );
		query.setString(0, "my id");
	   	list = query.list();
		assertTrue(list.size()==1);

		session.clear();

		query = session.createSQLQuery( "select s.id as {sing.id}, s.string_ as {sing.string}, s.prop as {sing.prop} from Single s where s.id = ?" )
			   .addEntity( "sing", Single.class );
		query.setString(0, "my id");
		list = query.list();

		assertTrue(list.size()==1);

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testReturnPropertyComponentRename() throws HibernateException, SQLException {
		// failure expected because this was a regression introduced previously which needs to get tracked down.
		Componentizable componentizable = setupComponentData();
		
		Session session = openSession();
		session.beginTransaction();
		Query namedQuery = session.getNamedQuery("queryComponentWithOtherColumn");
		List list = namedQuery.list();
		
		assertEquals(1, list.size());
		assertEquals( "flakky comp", ( (Componentizable) list.get(0) ).getComponent().getName() );
		
		session.clear();
		session.delete(componentizable);
		session.getTransaction().commit();
		session.close();
	}
	
	@Test
	public void testComponentStar() throws HibernateException, SQLException {
	    componentTest("select {comp.*} from Componentizable comp");
	}
	
	@Test
	public void testComponentNoStar() throws HibernateException, SQLException {
	    componentTest("select comp.id as {comp.id}, comp.nickName as {comp.nickName}, comp.name as {comp.component.name}, comp.subName as {comp.component.subComponent.subName}, comp.subName1 as {comp.component.subComponent.subName1} from Componentizable comp");
	}

	private void componentTest(String sql) throws SQLException {
	    Componentizable c = setupComponentData();

		Session session = openSession();
		session.beginTransaction();
		SQLQuery q = session.createSQLQuery( sql ).addEntity( "comp", Componentizable.class );
	    List list = q.list();
	    assertEquals(list.size(),1);

	    Componentizable co = (Componentizable) list.get(0);
	    assertEquals(c.getNickName(), co.getNickName());
	    assertEquals(c.getComponent().getName(), co.getComponent().getName());
	    assertEquals(c.getComponent().getSubComponent().getSubName(), co.getComponent().getSubComponent().getSubName());

	    session.delete( co );
		session.getTransaction().commit();
	    session.close();
    }

	private Componentizable setupComponentData() throws SQLException {
		Session session = sessionFactory().openSession();
		session.beginTransaction();

		Componentizable c = new Componentizable();
	    c.setNickName("Flacky");
	    Component component = new Component();
	    component.setName("flakky comp");
	    SubComponent subComponent = new SubComponent();
	    subComponent.setSubName("subway");
        component.setSubComponent(subComponent);
	    
        c.setComponent(component);
        
        session.save(c);
		session.getTransaction().commit();
        session.clear();

		return c;
	}

	@Test
	@SkipForDialect( MySQLDialect.class )
    public void testFindSimpleBySQL() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		Category s = new Category();
		s.setName(String.valueOf(nextLong++));
		session.save(s);
		session.flush();

		Query query = session.createSQLQuery( "select s.category_key_col as {category.id}, s.name as {category.name}, s.\"assign-able-id\" as {category.assignable} from {category} s" )
				.addEntity( "category", Category.class );
		List list = query.list();

		assertNotNull(list);
		assertTrue(list.size() > 0);
		assertTrue(list.get(0) instanceof Category);
		session.getTransaction().commit();
		session.close();
		// How do we handle objects with composite id's ? (such as Single)
	}

	@Test
	public void testFindBySQLSimpleByDiffSessions() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		Category s = new Category();
		s.setName(String.valueOf(nextLong++));
		session.save(s);
		session.getTransaction().commit();
		session.close();

		if ( getDialect() instanceof MySQLDialect ) {
			return;
		}

		session = openSession();
		session.beginTransaction();

		Query query = session.createSQLQuery( "select s.category_key_col as {category.id}, s.name as {category.name}, s.\"assign-able-id\" as {category.assignable} from {category} s" )
				.addEntity( "category", Category.class );
		List list = query.list();

		assertNotNull(list);
		assertTrue(list.size() > 0);
		assertTrue(list.get(0) instanceof Category);

		// How do we handle objects that does not have id property (such as Simple ?)
		// How do we handle objects with composite id's ? (such as Single)
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testFindBySQLDiscriminatedSameSession() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from A" ).list() ) {
			session.delete( entity );
		}
		A savedA = new A();
		session.save(savedA);

		B savedB = new B();
		session.save(savedB);
		session.flush();

		Query query = session.createSQLQuery( "select identifier_column as {a.id}, clazz_discriminata as {a.class}, name as {a.name}, count_ as {a.count} from TA {a}" )
				.addEntity( "a", A.class );
		List list = query.list();

		assertNotNull(list);
		assertEquals(2, list.size());

		A a1 = (A) list.get(0);
		A a2 = (A) list.get(1);

		assertTrue((a2 instanceof B) || (a1 instanceof B));
		assertFalse(a1 instanceof B && a2 instanceof B);

		if (a1 instanceof B) {
			assertSame(a1, savedB);
			assertSame(a2, savedA);
		}
		else {
			assertSame(a2, savedB);
			assertSame(a1, savedA);
		}

		session.clear();
		List list2 = session.getNamedQuery("propertyResultDiscriminator").list();
		assertEquals(2, list2.size());
		
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testFindBySQLDiscriminatedDiffSession() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		for ( Object entity : session.createQuery( "from A" ).list() ) {
			session.delete( entity );
		}
		A savedA = new A();
		session.save(savedA);

		B savedB = new B();
		session.save(savedB);
		session.getTransaction().commit();
		int count = session.createQuery("from A").list().size();
		session.close();

		session = openSession();
		session.beginTransaction();
		Query query = session.createSQLQuery( "select identifier_column as {a.id}, clazz_discriminata as {a.class}, count_ as {a.count}, name as {a.name} from TA" )
				.addEntity( "a", A.class );
		List list = query.list();

		assertNotNull(list);
		assertEquals(count, list.size());
		session.getTransaction().commit();
		session.close();
	}

	public static class DoubleQuoteDialect implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return '"' == dialect.openQuote() && '"' == dialect.closeQuote();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-21" )
	// because the XML mapping defines the loader for CompositeIdId using a column name that needs to be quoted
	@RequiresDialectFeature( DoubleQuoteDialect.class )
    public void testCompositeIdId() throws HibernateException, SQLException {
        Session s = openSession();
		s.beginTransaction();
        CompositeIdId id = new CompositeIdId();
        id.setName("Max");
        id.setSystem("c64");
        id.setId("games");
        s.save(id);
		s.getTransaction().commit();
        s.close();

        s = openSession();
		s.beginTransaction();
        // having a composite id with one property named id works since the map used by sqlloader to map names to properties handles it.
		// NOTE : SYSTEM is an ANSI SQL defined keyword, so it gets quoted; so it needs to get quoted here too
		String sql = String.format(
				"select %1$s as {c.system}, " +
						"  id as {c.id}, name as {c.name}, " +
						"  foo as {c.composite.foo}, " +
						"  bar as {c.composite.bar} " +
						"from CompositeIdId " +
						"where %1$s=? and id=?",
				getDialect().openQuote() + "system" + getDialect().closeQuote()
		);

		SQLQuery query = s.createSQLQuery( sql ).addEntity( "c", CompositeIdId.class );
        query.setString(0, "c64");
        query.setString(1, "games");

        CompositeIdId id2 = (CompositeIdId) query.uniqueResult();
        check(id, id2);

		s.getTransaction().commit();
        s.close();

        s = openSession();
		s.beginTransaction();
        CompositeIdId useForGet = new CompositeIdId();
        useForGet.setSystem("c64");
        useForGet.setId("games");
        // this doesn't work since the verification does not take column span into respect!
        CompositeIdId getted = (CompositeIdId) s.get(CompositeIdId.class, useForGet);
        check(id,getted);
		s.getTransaction().commit();
        s.close();
    }

    private void check(CompositeIdId id, CompositeIdId id2) {
        assertEquals(id,id2);
        assertEquals(id.getName(), id2.getName());
        assertEquals(id.getId(), id2.getId());
        assertEquals(id.getSystem(), id2.getSystem());
    }

}
