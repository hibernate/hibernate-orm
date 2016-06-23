package org.hibernate.test.fetchprofiles.cycle;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.test.annotations.fetch.Branch;
import org.hibernate.test.annotations.fetch.Leaf;
import org.hibernate.test.annotations.fetch.Person;
import org.hibernate.test.annotations.fetch.Stay;
import org.hibernate.test.annotations.fetchprofile.FetchProfileTest;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


@TestForIssue( jiraKey = "HHH-10745" )
public class FetchProfileCycleTest extends BaseCoreFunctionalTestCase {

	private static final Logger log = Logger.getLogger( FetchProfileCycleTest.class );
//	private ServiceRegistry serviceRegistry;
//	private SessionFactory sessionFactory;
//
//	@Before
//    public void setUp() {
//		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
//		Configuration config = new Configuration();
//		config.addAnnotatedClass(Start.class);
//		config.addAnnotatedClass(Via1.class);
//		config.addAnnotatedClass(Via2.class);
//		config.addAnnotatedClass(Mid.class);
//		config.addAnnotatedClass(Finish.class);
//		this.sessionFactory = (SessionFactoryImplementor) config.buildSessionFactory(serviceRegistry);
//	}
//
//	@After
//    public void tearDown() {
//		this.sessionFactory.close();
//        if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
//	}

	@Test
	public void testFetchProfileConfigured() {

		long start1Id = this.createSampleData(true);
		long start2Id = this.createSampleData(false);
		Start[] starts = this.fetchLoad(start1Id, start2Id);
		log.info("Start 0:" + starts[0]);
		log.info("Start 1:" + starts[1]);
		starts[0].getVia1().getMid().getFinish().getSumdumattr();
		starts[1].getVia2().getMid().getFinish().getSumdumattr();
	}

	public long createSampleData(boolean path1)
	{
		Session ss = this.openSession( );
		ss.getTransaction().begin();
		Finish f = new Finish();
		f.setSumdumattr("sumdumval");
		ss.persist(f);
		Mid m = new Mid();
		m.setFinish(f);
		ss.persist(m);
		Start s = new Start();
		ss.persist(s);
		if (path1)
		{
			Via1 v1 = new Via1();
			ss.persist(v1);
			s.setVia1(v1);
			v1.setMid(m);
		} else
		{
			Via2 v2 = new Via2();
			ss.persist(v2);
			s.setVia2(v2);
			v2.setMid(m);
		}
		ss.flush();
		ss.getTransaction().commit();
		ss.close();
		return s.getId();
	}
	public Start[] fetchLoad(long id1, long id2)
	{
		Session s = this.openSession( );
		s.enableFetchProfile("fp");
		Start s1 = s.get(Start.class, id1);
		Start s2 = s.get(Start.class, id2);
		s.disableFetchProfile("fp");
		s.close();
		return new Start[] {s1, s2};
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Start.class,
				Mid.class,
				Finish.class,
				Via1.class,
				Via2.class
		};
	}

}
