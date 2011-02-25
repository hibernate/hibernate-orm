package org.hibernate.test.cfg;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.Vector;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import junit.framework.AssertionFailedError;
import junit.framework.Protectable;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class AnnotationBinderTest extends TestCase {

		public static final Logger log = LoggerFactory.getLogger( AnnotationBinderTest.class );

		public AnnotationBinderTest() {
		}

		public AnnotationBinderTest(String name) {
			super( name );
		}

		/**
		 *
		 *  We are using an extended version of FunctionalTestClassTestSuite class to 
		 * be able to override some of junit 3 behaviors- mainly, we want to muffler 
		 * the exception that is thrown.
		 */
		public static Test suite() {
			AnnotationBinderTest t = new AnnotationBinderTest();
			AnnotationBinderTest.FunctionalTestClassTestSuiteExtended ts = t.new FunctionalTestClassTestSuiteExtended(AnnotationBinderTest.class);
			return ts;
		}

		/**
		 *  We need this method so that this class is considered a unit test. The real test
		 *  is embedded in the overridden buildConfiguration() method. 
		 */
		public void testAnnotations() {

		}
		
		@Override
		protected Class<?>[] getAnnotatedClasses() {
			return new Class[] {};
		}
		
		/**
		 *  this is the method that does the actual testing.
		 */
		@Override	
		protected void buildConfiguration() throws Exception {
			if ( sessions != null ) {
				sessions.close();
			}
			setCfg( new AnnotationConfiguration() );
			cfg.setProperty( AnnotationConfiguration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				( ( AnnotationConfiguration ) getCfg() ).addPackage( aPackage );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			
			for ( Class<?> aClass : getClassesWithCorrectMetada() ) {
				( ( AnnotationConfiguration ) getCfg() ).addAnnotatedClass( aClass );
			}
			

			// Let's build a session factory: the annotated class has proper meta data; therefore
			// the buildSessionFactory() method will not throw an exception
			sessions = getCfg().buildSessionFactory();

			// Here the annotated class does not have proper meta data; therefore
			// the buildSessionFactory() method will throw an exception
			for ( Class<?> aClass : getClassesWithImproperMetada() ) {
				( ( AnnotationConfiguration ) getCfg() ).addAnnotatedClass( aClass );
			}
			AnnotationException ae = null;
			try {
				sessions = getCfg().buildSessionFactory();					
			} catch (AnnotationException e ) {
				ae = e;
			}
			assertNotNull(ae);
			assertEquals("@MapKeyJoinColumn and @MapKeyJoinColumns used on the " +
					"same property: org.hibernate.test.cfg.AnnotationBinderTest" +
					"$ClassAtFault.names", ae.getMessage());
		}		
		
		protected Class<?>[] getClassesWithCorrectMetada() {
			return new Class[] {ClassWithProperMetadata1.class, ClassWithProperMetadata2.class};
		}
		
		protected Class<?>[] getClassesWithImproperMetada() {
			return new Class[] {ClassAtFault.class};
		}
		
		@Entity
		public class ClassWithProperMetadata1 {
			
			@SuppressWarnings("unused")
			@Id
			@GeneratedValue
			private int id;
			@ElementCollection
			@MapKeyJoinColumn
			private Set<String> names;
			
			public ClassWithProperMetadata1() {
			}
	
			public ClassWithProperMetadata1(Set<String> names) {
				this.names = names;
			}
			
			public void setName(Set<String> names) {
				this.names = names;
			}
			public Set<String> getNames() {
				return names;
			}
			
		}
		
		@Entity
		public class ClassWithProperMetadata2 {
			
			@SuppressWarnings("unused")
			@Id
			@GeneratedValue
			private int id;
			@ElementCollection
			@MapKeyJoinColumns(value = { @MapKeyJoinColumn })
			private Set<String> names;
			
			public ClassWithProperMetadata2() {
			}
	
			public ClassWithProperMetadata2(Set<String> names) {
				this.names = names;
			}
			
			public void setName(Set<String> names) {
				this.names = names;
			}
			public Set<String> getNames() {
				return names;
			}
			
		}

		@Entity
		public class ClassAtFault {
			@SuppressWarnings("unused")
			@Id
			@GeneratedValue
			private int id;
			@ElementCollection
			@MapKeyJoinColumns(value = { @MapKeyJoinColumn })
			@MapKeyJoinColumn
			private Set<String> names;
			
			public ClassAtFault() {
			}
	
			public ClassAtFault(Set<String> names) {
				this.names = names;
			}
			
			public void setName(Set<String> names) {
				this.names = names;
			}
			public Set<String> getNames() {
				return names;
			}
			
		}
		
		/**
		 *  Let's override the junit 3's lack of support for a test throwing exceptions		
		 */
		public class FunctionalTestClassTestSuiteExtended extends FunctionalTestClassTestSuite {
			public FunctionalTestClassTestSuiteExtended(Class testClass) {
				super(testClass);
			}
			@Override
			public void run(TestResult testResult) {
				try {
					log.info( "Starting test-suite [" + getName() + "]" );
					setUp();
					AnnotationBinderTest t = new AnnotationBinderTest();
					AnnotationBinderTest.TestResultExtended tr = t.new TestResultExtended();
					Field fListners;
					Vector listeners = null;
					try {
						fListners = TestResult.class.getDeclaredField("fListeners");
						fListners.setAccessible(true);
						listeners = (Vector) fListners.get(testResult);
			            fListners.setAccessible(false);
					} catch (Exception e) {
					}
					for (int i=0; i<listeners.size(); i++) {
						tr.addListener((TestListener) listeners.get(i));
					}
					super.run( tr );  // run with the extended version of TestResult
				}
				finally {
					try {
						tearDown();
					}
					catch( Throwable ignore ) {
					}
					log.info( "Completed test-suite [" + getName() + "]" );
				}
			}
		}
		/**
		 *  Let's override the junit 3's lack of support for a test throwing exceptions		
		 */
		public class TestResultExtended extends TestResult {
			public TestResultExtended() {
				super();
			}
			/**
			 * Runs a TestCase.
			 */
			@Override
			public void runProtected(final Test test, Protectable p) {
				try {
					p.protect();
				} 
				catch (AssertionFailedError e) {
					addFailure(test, e);
				}
				catch (ThreadDeath e) { 
					throw e;
				}
				catch (Throwable e) {
					//addError(test, e);    Commented out to ignore the exception
				}
			}
		}
}


