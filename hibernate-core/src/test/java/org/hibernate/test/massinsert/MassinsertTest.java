package org.hibernate.test.massinsert;

import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.id.enhanced.SequenceStyleConfigUnitTest.PooledSequenceDialect;
import org.hibernate.id.enhanced.SequenceStyleConfigUnitTest.SequenceDialect;
import org.hibernate.id.enhanced.SequenceStyleConfigUnitTest.TableDialect;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author chammer
 *
 */
public class MassinsertTest extends BaseCoreFunctionalTestCase {

	// private static final Logger log = Logger.getLogger(MassinsertTest.class);
	int amount = 1000;
	boolean delete=true;
	public MassinsertTest() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PooledSequenceDialect.class.getName() )
				.build();
		try {
			
			// optimizer=pooled w/ increment > 1 => hilo
			Properties props = buildGeneratorPropertiesBase( serviceRegistry );
						props.setProperty( SequenceStyleGenerator.OPT_PARAM, StandardOptimizerDescriptor.POOLED.getExternalName() );
						props.setProperty( SequenceStyleGenerator.INCREMENT_PARAM, "20" );
						SequenceStyleGenerator generator = new SequenceStyleGenerator();
						generator.configure( StandardBasicTypes.LONG, props, serviceRegistry );
						generator.registerExportables(
								new Database( new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry ) )
						);
						// because the dialect reports to not support pooled seqyences, the expectation is that we will
						// use a table for the backing structure...
						assertClassAssignability( SequenceStructure.class, generator.getDatabaseStructure().getClass() );
						assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
						assertEquals( 20, generator.getOptimizer().getIncrementSize() );
						assertEquals( 20, generator.getDatabaseStructure().getIncrementSize() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

	int var=1;
	}
	
	
	private Properties buildGeneratorPropertiesBase(StandardServiceRegistry serviceRegistry) {
		Properties props = new Properties();
		props.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				new MetadataBuildingContextTestingImpl( serviceRegistry ).getObjectNameNormalizer()
		);
		return props;
	}
	/**
	 * MyLog
	 */
	@Test
	public void massinsert_MyLog_Test() {
		List<MyLog> testData = generateTestDataMyLog(amount);
		InsertList( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	@Ignore( "named queries for insert seem generally not to work" )
	@Test
	public void massinsert_MyLog_Test_namedquery() {
		List<MyLog> testData = generateTestDataMyLog(amount);
		InsertList_namedquery( testData.toArray(),"myloginsert" );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}
	
	@Test
	public void massinsert_MyLog_Test_stateless() {
		List<MyLog> testData = generateTestDataMyLog(amount);
		InsertList_stateless( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}


	@Ignore( "named queries for insert seem generally not to work" )
	@Test
	public void massinsert_MyLog_Test_stateless_namedquery() {
		List<MyLog> testData = generateTestDataMyLog(amount);
		InsertList_stateless_namedquery( testData.toArray(), "myloginsert" );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}
	/**
	 * MyLog2
	 */
	@Test
	public void massinsert_MyLog2_Test() {
		List<MyLog2> testData = generateTestDataMyLog2(amount);
		InsertList( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog2 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	@Test
	public void massinsert_MyLog2_Test_stateless() {
		List<MyLog2> testData = generateTestDataMyLog2(amount);
		InsertList_stateless( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog2 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	@Ignore( "named queries for insert seem generally not to work" )
	@Test
	public void massinsert_MyLog2_Test_stateless_namedquery() {
		List<MyLog2> testData = generateTestDataMyLog2(amount);
		InsertList_stateless_namedquery( testData.toArray(), "mylog2insert" );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog2 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	
	/**
	 * MyLog3
	 */
	@Test
	public void massinsert_MyLog3_Test() {
		List<MyLog3> testData = generateTestDataMyLog3(amount);
		InsertList( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog3 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	@Test
	public void massinsert_MyLog3_Test_stateless() {
		List<MyLog3> testData = generateTestDataMyLog3(amount);
		InsertList_stateless( testData.toArray() );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog3 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}

	@Ignore( "named queries for insert seem generally not to work" )
	@Test
	public void massinsert_MyLog3_Test_stateless_namedquery() {
		List<MyLog3> testData = generateTestDataMyLog3(amount);
		InsertList_stateless_namedquery( testData.toArray(), "mylog3insert" );
		Session mysession = this.openSession();
		Object uniqueResult = mysession.createQuery( "Select count(j) from MyLog3 as j" ).uniqueResult();
		mysession.close();
		assertEquals(Long.valueOf( amount),uniqueResult);
		if(delete)deleteentries( testData.toArray() );
	}
	
	public void InsertList(Object[] objects) {
		Session session = null;
		Transaction tx = null;
		session = sessionFactory().openSession();
		tx = session.beginTransaction();
		for ( int i = 0; i < objects.length; i++ ) {
			// session.save(list.get(i));
			session.persist( objects[i] );
			// flush the session after 20 objects to avoid growing too much
			// see
			// http://www.hibernate.org/hib_docs/reference/en/html/batch.html
			if ( ( i + 1 ) % 20 == 0 ) { // 20, same as the JDBC batch size
				// flush a batch of inserts and release memory:
				session.flush();
				session.clear();
			}
		}
		tx.commit();
		session.close();
	}
	
	public void InsertList_namedquery(Object[] objects, String namedqueryname) {
		Session session = null;
		Transaction tx = null;
		session = sessionFactory().openSession();
		tx = session.beginTransaction();
		for ( Object myLog : objects ) {
			Query namedQuery = session.getNamedQuery( namedqueryname );
			namedQuery.setString( "text", ((IContainer)myLog).getText() );
			namedQuery.executeUpdate();
		}
		tx.commit();
		session.close();
	}
	
	public void InsertList_stateless(Object[] objects) {
		StatelessSession session = null;
		Transaction tx = null;
		session = sessionFactory().openStatelessSession();
		tx = session.beginTransaction();
		for ( int i = 0; i < objects.length; i++ ) {
			session.insert( objects[i] );
		}
		tx.commit();
		session.close();
	}

	public void InsertList_stateless_namedquery(Object[] objects, String namedqueryname) {
		StatelessSession session = null;
		Transaction tx = null;
		session = sessionFactory().openStatelessSession();
		tx = session.beginTransaction();
		for ( Object myLog : objects ) {
			Query namedQuery = session.getNamedQuery( namedqueryname );
			namedQuery.setString( "text", ((IContainer)myLog).getText() );
			namedQuery.executeUpdate();
		}
		tx.commit();
		session.close();
	}

	protected List<MyLog> generateTestDataMyLog(int numberoflogentries) {
		List<MyLog> arl=new ArrayList<MyLog>(numberoflogentries);
		for (int i = 0; i < numberoflogentries; i++) {
			MyLog myLog = new MyLog();
			myLog.setText( "nr:" + i );
			arl.add( myLog);
		}
		return arl;
	}
	
	protected List<MyLog2> generateTestDataMyLog2(int numberoflogentries) {
		List<MyLog2> arl=new ArrayList<MyLog2>(numberoflogentries);
		for (int i = 0; i < numberoflogentries; i++) {
			MyLog2 myLog = new MyLog2();
			myLog.setText( "nr:" + i );
			arl.add( myLog);
		}
		return arl;
	}
	
	protected List<MyLog3> generateTestDataMyLog3(int numberoflogentries) {
		List<MyLog3> arl=new ArrayList<MyLog3>(numberoflogentries);
		for (int i = 0; i < numberoflogentries; i++) {
			MyLog3 myLog = new MyLog3();
			myLog.setText( "nr:" + i );
			arl.add( myLog);
		}
		return arl;
	}

	protected void deleteentries(Object[] objects) {
		StatelessSession statelessSession = sessionFactory().openStatelessSession();
		Transaction tx = statelessSession.beginTransaction();
		for ( Object mylog : objects ) {
			statelessSession.delete( mylog );
		}
		tx.commit();
		statelessSession.close();
	}
	
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				MyLog.class,
				MyLog2.class,
				MyLog3.class
		};
	}
}
