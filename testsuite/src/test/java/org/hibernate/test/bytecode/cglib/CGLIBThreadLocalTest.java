package org.hibernate.test.bytecode.cglib;

import org.hibernate.junit.functional.*;
import org.hibernate.cfg.*;
import org.hibernate.*;
import org.hibernate.proxy.*;
import org.hibernate.test.bytecode.*;
import junit.framework.*;

import java.lang.reflect.*;


/**
 * Test that the static thread local callback object is cleared out of the proxy class after instantiated.
 * This tests that the memory leak reported by HHH-2481 hasn't been re-introduced.
 *
 * @author Paul Malolepsy
 */
public class CGLIBThreadLocalTest extends FunctionalTestCase {
	public CGLIBThreadLocalTest(String name) {
		super(name);
	}

	public String[] getMappings() {
		return new String[]{"bytecode/Bean.hbm.xml"};
	}

	public static TestSuite suite() {
		return new FunctionalTestClassTestSuite(CGLIBThreadLocalTest.class);
	}

	public void testCglibClearing() {
		if (!(Environment.getBytecodeProvider() instanceof org.hibernate.bytecode.cglib.BytecodeProviderImpl)) {
			// because of the scoping :(
			reportSkip("env not configured for cglib provider", "cglib thread local callback clearing");
			return;
		}

		//create the object for the test
		Session s = openSession();
		s.beginTransaction();
		ProxyBean proxyBean = new ProxyBean();
		proxyBean.setSomeString("my-bean");
		proxyBean.setSomeLong(1234);
		s.save(proxyBean);
		s.getTransaction().commit();
		s.close();

		// read the object as a proxy
		s = openSession();
		s.beginTransaction();
		proxyBean = (ProxyBean) s.load(ProxyBean.class, proxyBean.getSomeString());
		assertTrue(proxyBean instanceof HibernateProxy);
		try {
			//check that the static thread callbacks thread local has been cleared out
			Field field = proxyBean.getClass().getDeclaredField("CGLIB$THREAD_CALLBACKS");
			field.setAccessible(true);
			ThreadLocal threadCallbacksThreadLocal = (ThreadLocal) field.get(null);
			assertTrue(threadCallbacksThreadLocal.get() == null);
		} catch (NoSuchFieldException e1) {
			fail("unable to find CGLIB$THREAD_CALLBACKS field in proxy.");
		} catch (Throwable t) {
			fail("unexpected exception type : " + t);
		} finally {
			//clean up
			s.delete(proxyBean);
			s.getTransaction().commit();
			s.close();
		}
	}
}
