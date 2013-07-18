package org.hibernate.testing.junit4;

import org.junit.After;
import org.junit.Before;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class BaseCoreFunctionalTestMethod extends BaseFunctionalTestCase {
	private TestSessionFactoryHelper sessionFactoryHelper;

	@Before
	public final void beforeTest() throws Exception {
		setTestConfiguration( new TestConfigurationHelper() );
		setTestServiceRegistryHelper( new TestServiceRegistryHelper( getTestConfiguration() ) );
		sessionFactoryHelper = new TestSessionFactoryHelper(
				getTestServiceRegistryHelper(), getTestConfiguration()
		);
		initialize();
		prepareTest();
	}

	protected void prepareTest() throws Exception {
	}

	@After
	public final void afterTest() throws Exception {
		prepareCleanup();
		if ( getSessionFactoryHelper() != null ) {
			getSessionFactoryHelper().destory();
		}
		setSessionFactoryHelper( null );
		if( getTestServiceRegistryHelper()!=null){
			getTestServiceRegistryHelper().destroy();
		}
		setTestServiceRegistryHelper( null );
		setTestConfiguration( null );
	}

	protected void prepareCleanup() {

	}

	public TestSessionFactoryHelper getSessionFactoryHelper() {
		return sessionFactoryHelper;
	}

	public void setSessionFactoryHelper(final TestSessionFactoryHelper sessionFactoryHelper) {
		this.sessionFactoryHelper = sessionFactoryHelper;
	}
}
