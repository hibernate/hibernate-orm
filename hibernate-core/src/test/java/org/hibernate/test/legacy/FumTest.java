/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: FumTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MckoiDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PointbaseDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.TimesTenDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.transform.Transformers;
import org.hibernate.type.CalendarType;
import org.hibernate.type.EntityType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.junit.Test;

public class FumTest extends LegacyTestCase {
	private static short fumKeyShort = 1;

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	public String[] getMappings() {
		return new String[] {
			"legacy/FooBar.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Fumm.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Immutable.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Vetoer.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/Location.hbm.xml",
			"legacy/Stuff.hbm.xml",
			"legacy/Container.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/Middle.hbm.xml"
		};
	}

	@Test
	public void testQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from Fum fum where fum.fo.id.string = 'x'").list();
		t.commit();
		s.close();
	}

	@Test
	public void testCriteriaCollection() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Fum fum = new Fum( fumKey("fum") );
		fum.setFum("a value");
		fum.getMapComponent().getFummap().put("self", fum);
		fum.getMapComponent().getStringmap().put("string", "a staring");
		fum.getMapComponent().getStringmap().put("string2", "a notha staring");
		fum.getMapComponent().setCount(1);
		s.save(fum);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Fum b = (Fum) s.createCriteria(Fum.class).add(
			Restrictions.in("fum", new String[] { "a value", "no value" } )
		)
		.uniqueResult();
		assertTrue( Hibernate.isInitialized( b.getMapComponent().getStringmap() ) );
		assertTrue( b.getMapComponent().getFummap().size()==1 );
		assertTrue( b.getMapComponent().getStringmap().size()==2 );
		s.delete(b);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCriteria() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Fum fum = new Fum( fumKey("fum") );
		fum.setFo( new Fum( fumKey("fo") ) );
		fum.setFum("fo fee fi");
		fum.getFo().setFum("stuff");
		Fum fr = new Fum( fumKey("fr") );
		fr.setFum("goo");
		Fum fr2 = new Fum( fumKey("fr2") );
		fr2.setFum("soo");
		fum.setFriends( new HashSet() );
		fum.getFriends().add(fr);
		fum.getFriends().add(fr2);
		s.save(fr);
		s.save(fr2);
		s.save( fum.getFo() );
		s.save(fum);

		Criteria base = s.createCriteria(Fum.class)
			.add( Restrictions.like("fum", "f", MatchMode.START) );
		base.createCriteria("fo")
			.add( Restrictions.isNotNull("fum") );
		base.createCriteria("friends")
			.add( Restrictions.like("fum", "g%") );
		List list = base.list();
		assertTrue( list.size()==1 && list.get(0)==fum );

		base = s.createCriteria(Fum.class)
			.add( Restrictions.like("fum", "f%") )
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
		base.createCriteria("fo", "fo")
			.add( Restrictions.isNotNull("fum") );
		base.createCriteria("friends", "fum")
			.add( Restrictions.like("fum", "g", MatchMode.START) );
		Map map = (Map) base.uniqueResult();

		assertTrue(
			map.get("this")==fum &&
			map.get("fo")==fum.getFo() &&
			fum.getFriends().contains( map.get("fum") ) &&
			map.size()==3
		);

		base = s.createCriteria(Fum.class)
			.add( Restrictions.like("fum", "f%") )
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
			.setFetchMode( "friends", FetchMode.JOIN );
		base.createCriteria("fo", "fo")
			.add( Restrictions.eq( "fum", fum.getFo().getFum() ) );
		map = (Map) base.list().get(0);

		assertTrue(
			map.get("this")==fum &&
			map.get("fo")==fum.getFo() &&
			map.size()==2
		);

		list = s.createCriteria(Fum.class)
			.createAlias("friends", "fr")
			.createAlias("fo", "fo")
			.add( Restrictions.like("fum", "f%") )
			.add( Restrictions.isNotNull("fo") )
			.add( Restrictions.isNotNull("fo.fum") )
			.add( Restrictions.like("fr.fum", "g%") )
			.add( Restrictions.eqProperty("fr.id.short", "id.short") )
			.list();
		assertTrue( list.size()==1 && list.get(0)==fum );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		base = s.createCriteria(Fum.class)
			.add( Restrictions.like("fum", "f%") );
		base.createCriteria("fo")
			.add( Restrictions.isNotNull("fum") );
		base.createCriteria("friends")
			.add( Restrictions.like("fum", "g%") );
		fum = (Fum) base.list().get(0);
		assertTrue(  fum.getFriends().size()==2 );
		s.delete(fum);
		s.delete( fum.getFo() );
		Iterator iter = fum.getFriends().iterator();
		while ( iter.hasNext() ) {
			s.delete( iter.next() );
		}
		txn.commit();
		s.close();
	}

	static public class ABean {
		public Fum fum;
		public Fum fo;
		public Fum getFo() {
			return fo;
		}
		public void setFo(Fum fo) {
			this.fo = fo;
		}
		public Fum getFum() {
			return fum;
		}
		public void setFum(Fum fum) {
			this.fum = fum;
		}
	}

	@Test
	public void testBeanResultTransformer() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction transaction = s.beginTransaction();
		Fum fum = new Fum( fumKey("fum") );
		fum.setFo( new Fum( fumKey("fo") ) );
		fum.setFum("fo fee fi");
		fum.getFo().setFum("stuff");
		Fum fr = new Fum( fumKey("fr") );
		fr.setFum("goo");
		Fum fr2 = new Fum( fumKey("fr2") );
		fr2.setFum("soo");
		fum.setFriends( new HashSet() );
		fum.getFriends().add(fr);
		fum.getFriends().add(fr2);
		s.save(fr);
		s.save(fr2);
		s.save( fum.getFo() );
		s.save(fum);
		
		Criteria test = s.createCriteria(Fum.class, "xam")
			.createCriteria("fo", "fo")
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
		
		Map fc = (Map) test.list().get(0);
		assertNotNull(fc.get("xam"));
		
		Criteria base = s.createCriteria(Fum.class, "fum")
		.add( Restrictions.like("fum", "f%") )
		.setResultTransformer(Transformers.aliasToBean(ABean.class))
		.setFetchMode("friends", FetchMode.JOIN);
		base.createCriteria("fo", "fo")
		.add( Restrictions.eq( "fum", fum.getFo().getFum() ) );
		ABean map = (ABean) base.list().get(0);

		assertTrue(
				map.getFum()==fum &&
				map.getFo()==fum.getFo() );
		
		s.delete(fr);
		s.delete(fr2);
		s.delete(fum);
		s.delete(fum.getFo());
		s.flush();
		transaction.commit();
		s.close();
	}

	@Test
	public void testListIdentifiers() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Fum fum = new Fum( fumKey("fum") );
		fum.setFum("fo fee fi");
		s.save(fum);
		fum = new Fum( fumKey("fi") );
		fum.setFum("fee fi fo");
		s.save(fum);
		List list = s.createQuery( "select fum.id from Fum as fum where not fum.fum='FRIEND'" ).list();
		assertTrue( "list identifiers", list.size()==2);
		Iterator iter = s.createQuery( "select fum.id from Fum fum where not fum.fum='FRIEND'" ).iterate();
		int i=0;
		while ( iter.hasNext() ) {
			assertTrue( "iterate identifiers",  iter.next() instanceof FumCompositeID);
			i++;
		}
		assertTrue(i==2);

		s.delete( s.load(Fum.class, (Serializable) list.get(0) ) );
		s.delete( s.load(Fum.class, (Serializable) list.get(1) ) );
		txn.commit();
		s.close();
	}


	public static FumCompositeID fumKey(String str) {
		return fumKey(str,false);
	}

	private static FumCompositeID fumKey(String str, boolean aCompositeQueryTest) {
		FumCompositeID id = new FumCompositeID();
		id.setString( str );

		if (aCompositeQueryTest) {
			id.setShort( fumKeyShort++ );
		}
		else {
			id.setShort( (short) 12 );
		}

		return id;
	}

	@Test
	public void testCompositeID() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		FumCompositeID fumKey = fumKey("fum");
		Fum fum = new Fum( fumKey );
		fum.setFum("fee fi fo");
		s.save(fum);
		assertTrue( "load by composite key", fum==s.load( Fum.class, fumKey ) );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		if ( getDialect() instanceof AbstractHANADialect ){
			// HANA currently requires specifying table name by 'FOR UPDATE of t1.c1' if there are more than one tables/views/subqueries in the FROM clause
			fum = (Fum) s.load( Fum.class, fumKey );
		} else {
			fum = (Fum) s.load( Fum.class, fumKey, LockMode.UPGRADE );
		}

		assertTrue( "load by composite key", fum!=null );

		FumCompositeID fumKey2 = fumKey("fi");
		Fum fum2 = new Fum( fumKey2 );
		fum2.setFum("fee fo fi");
		fum.setFo(fum2);
		s.save(fum2);
		assertTrue(
			"find composite keyed objects",
				s.createQuery( "from Fum fum where not fum.fum='FRIEND'" ).list().size()==2
		);
		assertTrue(
			"find composite keyed object",
				s.createQuery( "select fum from Fum fum where fum.fum='fee fi fo'" ).list().get(0)==fum
		);
		fum.setFo(null);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		Iterator iter = s.createQuery( "from Fum fum where not fum.fum='FRIEND'" ).iterate();
		int i = 0;
		while ( iter.hasNext() ) {
			fum = (Fum) iter.next();
			//iter.remove();
			s.delete(fum);
			i++;
		}
		assertTrue( "iterate on composite key", i==2 );
		txn.commit();
		s.close();
	}

	@Test
	public void testCompositeIDOneToOne() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		FumCompositeID fumKey = fumKey("fum");
		Fum fum = new Fum( fumKey );
		fum.setFum("fee fi fo");
		//s.save(fum);
		Fumm fumm = new Fumm();
		fumm.setFum(fum);
		s.save(fumm);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		fumm = (Fumm) s.load( Fumm.class, fumKey );
		//s.delete( fumm.getFum() );
		s.delete(fumm);
		txn.commit();
		s.close();
	}

	@Test
	public void testCompositeIDQuery() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Fum fee = new Fum( fumKey("fee",true) );
		fee.setFum("fee");
		s.save(fee);
		Fum fi = new Fum( fumKey("fi",true) );
		fi.setFum("fi");
		short fiShort = fi.getId().getShort();
		s.save(fi);
		Fum fo = new Fum( fumKey("fo",true) );
		fo.setFum("fo");
		s.save(fo);
		Fum fum = new Fum( fumKey("fum",true) );
		fum.setFum("fum");
		s.save(fum);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// Try to find the Fum object "fo" that we inserted searching by the string in the id
		List vList = s.createQuery( "from Fum fum where fum.id.string='fo'" ).list();
		assertTrue( "find by composite key query (find fo object)", vList.size() == 1 );
		fum = (Fum)vList.get(0);
		assertTrue( "find by composite key query (check fo object)", fum.getId().getString().equals("fo") );

		// Try to find the Fum object "fi" that we inserted
		vList = s.createQuery( "from Fum fum where fum.id.short = ?" )
				.setParameter( 0, new Short(fiShort), StandardBasicTypes.SHORT )
				.list();
		assertEquals( "find by composite key query (find fi object)", 1, vList.size() );
		fi = (Fum)vList.get(0);
		assertEquals( "find by composite key query (check fi object)", "fi", fi.getId().getString() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		assertTrue(
				s.createQuery( "select fum.id.short, fum.id.string from Fum fum" ).iterate().hasNext()
		);
		assertTrue(
				s.createQuery( "select fum.id from Fum fum" ).iterate().hasNext()
		);
		Query qu = s.createQuery("select fum.fum, fum , fum.fum from Fum fum");
		Type[] types = qu.getReturnTypes();
		assertTrue(types.length==3);
		for ( int k=0; k<types.length; k++) {
			assertTrue( types[k]!=null );
		}
		assertTrue(types[0] instanceof StringType);
		assertTrue(types[1] instanceof EntityType);
		assertTrue(types[2] instanceof StringType);
		Iterator iter = qu.iterate();
		int j = 0;
		while ( iter.hasNext() ) {
			j++;
			assertTrue( ( (Object[]) iter.next() )[1] instanceof Fum );
		}
		assertTrue( "iterate on composite key", j==8 );

		fum = (Fum) s.load( Fum.class, fum.getId() );
		s.createFilter( fum.getQuxArray(), "where this.foo is null" ).list();
		s.createFilter( fum.getQuxArray(), "where this.foo.id = ?" )
				.setParameter( 0, "fooid", StandardBasicTypes.STRING )
				.list();
		Query f = s.createFilter( fum.getQuxArray(), "where this.foo.id = :fooId" );
		f.setString("fooId", "abc");
		assertFalse( f.iterate().hasNext() );

		iter = s.createQuery( "from Fum fum where not fum.fum='FRIEND'" ).iterate();
		int i = 0;
		while ( iter.hasNext() ) {
			fum = (Fum) iter.next();
			s.delete(fum);
			i++;
		}
		assertTrue( "iterate on composite key", i==4 );
		s.flush();

		s.createQuery( "from Fum fu, Fum fo where fu.fo.id.string = fo.id.string and fo.fum is not null" ).iterate();

		s.createQuery( "from Fumm f1 inner join f1.fum f2" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCompositeIDCollections() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Fum fum1 = new Fum( fumKey("fum1") );
		Fum fum2 = new Fum( fumKey("fum2") );
		fum1.setFum("fee fo fi");
		fum2.setFum("fee fo fi");
		s.save(fum1);
		s.save(fum2);
		Qux q = new Qux();
		s.save(q);
		Set set = new HashSet();
		List list = new ArrayList();
		set.add(fum1); set.add(fum2);
		list.add(fum1);
		q.setFums(set);
		q.setMoreFums(list);
		fum1.setQuxArray( new Qux[] {q} );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		q = (Qux) s.load( Qux.class, q.getKey() );
		assertTrue( "collection of fums", q.getFums().size()==2 );
		assertTrue( "collection of fums", q.getMoreFums().size()==1 );
		assertTrue( "unkeyed composite id collection", ( (Fum) q.getMoreFums().get(0) ).getQuxArray()[0]==q );
		Iterator iter = q.getFums().iterator();
		iter.hasNext();
		Fum f = (Fum) iter.next();
		s.delete(f);
		iter.hasNext();
		f = (Fum) iter.next();
		s.delete(f);
		s.delete(q);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testDeleteOwner() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Qux q = new Qux();
		s.save(q);
		Fum f1 = new Fum( fumKey("f1") );
		Fum f2 = new Fum( fumKey("f2") );
		Set set = new HashSet();
		set.add(f1);
		set.add(f2);
		List list = new LinkedList();
		list.add(f1);
		list.add(f2);
		f1.setFum("f1");
		f2.setFum("f2");
		q.setFums(set);
		q.setMoreFums(list);
		s.save(f1);
		s.save(f2);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		q = (Qux) s.load( Qux.class, q.getKey(), LockMode.UPGRADE );
		s.lock( q, LockMode.UPGRADE );
		s.delete(q);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		list = s.createQuery( "from Fum fum where not fum.fum='FRIEND'" ).list();
		assertTrue( "deleted owner", list.size()==2 );
		s.lock( list.get(0), LockMode.UPGRADE );
		s.lock( list.get(1), LockMode.UPGRADE );
		Iterator iter = list.iterator();
		while ( iter.hasNext() ) {
			s.delete( iter.next() );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCompositeIDs() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		FumCompositeID fumKey = fumKey("an instance of fo");
		Fo fo = Fo.newFo( fumKey );
		Properties props = new Properties();
		props.setProperty("foo", "bar");
		props.setProperty("bar", "foo");
		fo.setSerial(props);
		fo.setBuf( "abcdefghij1`23%$*^*$*\n\t".getBytes() );
		s.save( fo );
		s.flush();
		props.setProperty("x", "y");
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		fo = (Fo) s.load( Fo.class, fumKey );
		props = (Properties) fo.getSerial();
		assertTrue( props.getProperty("foo").equals("bar") );
		//assertTrue( props.contains("x") );
		assertTrue( props.getProperty("x").equals("y") );
		assertTrue( fo.getBuf()[0]=='a' );
		fo.getBuf()[1]=(byte)126;
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		fo = (Fo) s.load( Fo.class, fumKey );
		assertTrue( fo.getBuf()[1]==126 );
		assertTrue(
				s.createQuery( "from Fo fo where fo.id.string like 'an instance of fo'" ).iterate().next()==fo
		);
		s.delete(fo);
		s.flush();
		try {
			s.save( Fo.newFo() );
			assertTrue(false);
		}
		catch (Exception e) {
			//System.out.println( e.getMessage() );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testKeyManyToOne() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Inner sup = new Inner();
		InnerKey sid = new InnerKey();
		sup.setDudu("dudu");
		sid.setAkey("a");
		sid.setBkey("b");
		sup.setId(sid);
		Middle m = new Middle();
		MiddleKey mid = new MiddleKey();
		mid.setOne("one");
		mid.setTwo("two");
		mid.setSup(sup);
		m.setId(mid);
		m.setBla("bla");
		Outer d = new Outer();
		OuterKey did = new OuterKey();
		did.setMaster(m);
		did.setDetailId("detail");
		d.setId(did);
		d.setBubu("bubu");
		s.save(sup);
		s.save(m);
		s.save(d);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Inner in = (Inner) s.createQuery( "from Inner" ).list().get(0);
		assertTrue( in.getMiddles().size()==1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		assertTrue( s.createQuery( "from Inner _inner join _inner.middles middle" ).list().size()==1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		d = (Outer) s.load(Outer.class, did);
		assertTrue( d.getId().getMaster().getId().getSup().getDudu().equals("dudu") );
		s.delete(d);
		s.delete( d.getId().getMaster() );
		s.save( d.getId().getMaster() );
		s.save(d);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		d = (Outer) s.createQuery( "from Outer o where o.id.detailId = ?" )
				.setParameter( 0, d.getId().getDetailId(), StandardBasicTypes.STRING )
				.list()
				.get(0);
		s.createQuery( "from Outer o where o.id.master.id.sup.dudu is not null" ).list();
		s.createQuery( "from Outer o where o.id.master.id.sup.id.akey is not null" ).list();
		s.createQuery( "from Inner i where i.backOut.id.master.id.sup.id.akey = i.id.bkey" ).list();
		List l = s.createQuery( "select o.id.master.id.sup.dudu from Outer o where o.id.master.id.sup.dudu is not null" )
				.list();
		assertTrue(l.size()==1);
		l = s.createQuery( "select o.id.master.id.sup.id.akey from Outer o where o.id.master.id.sup.id.akey is not null" )
				.list();
		assertTrue(l.size()==1);
		s.createQuery(
				"select i.backOut.id.master.id.sup.id.akey from Inner i where i.backOut.id.master.id.sup.id.akey = i.id.bkey"
		).list();
		s.createQuery( "from Outer o where o.id.master.bla = ''" ).list();
		s.createQuery( "from Outer o where o.id.master.id.one = ''" ).list();
		s.createQuery( "from Inner inn where inn.id.bkey is not null and inn.backOut.id.master.id.sup.id.akey > 'a'" )
				.list();
		s.createQuery( "from Outer as o left join o.id.master m left join m.id.sup where o.bubu is not null" ).list();
		s.createQuery( "from Outer as o left join o.id.master.id.sup s where o.bubu is not null" ).list();
		s.createQuery( "from Outer as o left join o.id.master m left join o.id.master.id.sup s where o.bubu is not null" )
				.list();
		s.delete(d);
		s.delete( d.getId().getMaster() );
		s.delete( d.getId().getMaster().getId().getSup() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-3690" )
	public void testCompositeKeyPathExpressions() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select fum1.fo from Fum fum1 where fum1.fo.fum is not null" ).list();
		s.createQuery( "from Fum fum1 where fum1.fo.fum is not null order by fum1.fo.fum" ).list();
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) && !(getDialect() instanceof MckoiDialect) && !(getDialect() instanceof PointbaseDialect) ) {
			s.createQuery( "from Fum fum1 where exists elements(fum1.friends)" ).list();
			if(!(getDialect() instanceof TimesTenDialect)) { // can't execute because TimesTen can't do subqueries combined with aggreations
				s.createQuery( "from Fum fum1 where size(fum1.friends) = 0" ).list();
			}
		}
		s.createQuery( "select elements(fum1.friends) from Fum fum1" ).list();
		s.createQuery( "from Fum fum1, fr in elements( fum1.friends )" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUnflushedSessionSerialization() throws Exception {
		///////////////////////////////////////////////////////////////////////////
		// Test insertions across serializations
		Session s = sessionFactory().openSession();
		s.setFlushMode(FlushMode.MANUAL);
		s.beginTransaction();

		Simple simple = new Simple( Long.valueOf(10) );
		simple.setAddress("123 Main St. Anytown USA");
		simple.setCount(1);
		simple.setDate( new Date() );
		simple.setName("My UnflushedSessionSerialization Simple");
		simple.setPay( Float.valueOf(5000) );
		s.save( simple );

		// Now, try to serialize session without flushing...
		s.getTransaction().commit();
		Session s2 = spoofSerialization(s);
		s.close();
		s = s2;
		s.beginTransaction();

		simple = (Simple) s.load( Simple.class, new Long(10) );
		Simple other = new Simple( Long.valueOf(11) );
		other.init();
		s.save( other );

		simple.setOther(other);
		s.flush();

		s.getTransaction().commit();
		s.close();
		Simple check = simple;

		///////////////////////////////////////////////////////////////////////////
		// Test updates across serializations
		s = sessionFactory().openSession();
		s.setFlushMode(FlushMode.MANUAL);
		s.beginTransaction();

		simple = (Simple) s.get( Simple.class, Long.valueOf(10) );
		assertEquals( "Not same parent instances", check.getName(), simple.getName() );
		assertEquals( "Not same child instances", check.getOther().getName(), other.getName() );

		simple.setName("My updated name");

		s.getTransaction().commit();
		s2 = spoofSerialization(s);
		s.close();
		s = s2;
		s.beginTransaction();
		s.flush();

		s.getTransaction().commit();
		s.close();
		check = simple;

		///////////////////////////////////////////////////////////////////////////
		// Test deletions across serializations
		s = sessionFactory().openSession();
		s.setFlushMode(FlushMode.MANUAL);
		s.beginTransaction();

		simple = (Simple) s.get( Simple.class, Long.valueOf( 10 ) );
		assertEquals( "Not same parent instances", check.getName(), simple.getName() );
		assertEquals( "Not same child instances", check.getOther().getName(), other.getName() );

		// Now, lets delete across serialization...
		s.delete(simple);

		s.getTransaction().commit();
		s2 = spoofSerialization(s);
		s.close();
		s = s2;
		s.beginTransaction();
		s.flush();

		s.getTransaction().commit();
		s.close();

		///////////////////////////////////////////////////////////////////////////
		// Test collection actions across serializations
		s = sessionFactory().openSession();
		s.setFlushMode(FlushMode.MANUAL);
		s.beginTransaction();

		Fum fum = new Fum( fumKey("uss-fum") );
		fum.setFo( new Fum( fumKey("uss-fo") ) );
		fum.setFum("fo fee fi");
		fum.getFo().setFum("stuff");
		Fum fr = new Fum( fumKey("uss-fr") );
		fr.setFum("goo");
		Fum fr2 = new Fum( fumKey("uss-fr2") );
		fr2.setFum("soo");
		fum.setFriends( new HashSet() );
		fum.getFriends().add(fr);
		fum.getFriends().add(fr2);
		s.save(fr);
		s.save(fr2);
		s.save( fum.getFo() );
		s.save(fum);

		s.getTransaction().commit();
		s2 = spoofSerialization(s);
		s.close();
		s = s2;
		s.beginTransaction();
		s.flush();

		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.setFlushMode(FlushMode.MANUAL);
		s.beginTransaction();
		fum = (Fum) s.load( Fum.class, fum.getId() );

		assertEquals( "the Fum.friends did not get saved", 2, fum.getFriends().size() );

		fum.setFriends(null);
		s.getTransaction().commit();
		s2 = spoofSerialization(s);
		s.close();
		
		s = s2;
		s.beginTransaction();
		s.flush();

		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		s.setFlushMode(FlushMode.MANUAL);
		fum = (Fum) s.load( Fum.class, fum.getId() );
		assertTrue("the Fum.friends is not empty", fum.getFriends() == null || fum.getFriends().size() == 0);
		s.getTransaction().commit();
		s.close();
	}

	private Session spoofSerialization(Session session) throws IOException {
		try {
			// Serialize the incoming out to memory
			ByteArrayOutputStream serBaOut = new ByteArrayOutputStream();
			ObjectOutputStream serOut = new ObjectOutputStream(serBaOut);

			serOut.writeObject(session);

			// Now, re-constitute the model from memory
			ByteArrayInputStream serBaIn =
			        new ByteArrayInputStream(serBaOut.toByteArray());
			ObjectInputStream serIn = new ObjectInputStream(serBaIn);

			Session outgoing = (Session) serIn.readObject();

			return outgoing;
		}
		catch (ClassNotFoundException cnfe) {
			throw new IOException("Unable to locate class on reconstruction");
		}
	}

}







