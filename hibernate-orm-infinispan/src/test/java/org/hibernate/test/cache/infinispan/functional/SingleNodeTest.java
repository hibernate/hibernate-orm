package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.test.cache.infinispan.util.TxUtil;

import javax.transaction.TransactionManager;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class SingleNodeTest extends AbstractFunctionalTest {
	@Override
	protected void afterSessionFactoryBuilt(SessionFactoryImplementor sessionFactory) {
		super.afterSessionFactoryBuilt(sessionFactory);
		JtaPlatform jtaPlatform = sessionFactory().getServiceRegistry().getService(JtaPlatform.class);
		if (jtaPlatformClass != null) {
			assertNotNull(jtaPlatform);
			assertEquals(jtaPlatformClass, jtaPlatform.getClass());
		}
	}

	protected void withTxSession(TxUtil.ThrowingConsumer<Session, Exception> consumer) throws Exception {
		withTxSession(sessionFactory().withOptions(), consumer);
	}

	protected void withTxSession(SessionBuilder sessionBuilder, TxUtil.ThrowingConsumer<Session, Exception> consumer) throws Exception {
		JtaPlatform jtaPlatform = useJta ? sessionFactory().getServiceRegistry().getService(JtaPlatform.class) : null;
		TxUtil.withTxSession(jtaPlatform, sessionBuilder, consumer);
	}

	protected <T> T withTxSessionApply(TxUtil.ThrowingFunction<Session, T, Exception> function) throws Exception {
		JtaPlatform jtaPlatform = useJta ? sessionFactory().getServiceRegistry().getService(JtaPlatform.class) : null;
		return TxUtil.withTxSessionApply(jtaPlatform, sessionFactory().withOptions(), function);
	}

	protected <T> T withTx(Callable<T> callable) throws Exception {
		if (useJta) {
			TransactionManager tm = sessionFactory().getServiceRegistry().getService(JtaPlatform.class).retrieveTransactionManager();
			return Caches.withinTx(tm, () -> callable.call());
		} else {
			return callable.call();
		}
	}

	public <E extends Throwable> void withSession(TxUtil.ThrowingConsumer<Session, E> consumer) throws E {
		TxUtil.withSession(sessionFactory().withOptions(), consumer);
	}


	public <R, E extends Throwable> R withSessionApply(TxUtil.ThrowingFunction<Session, R, E> function) throws E {
		return TxUtil.withSessionApply(sessionFactory().withOptions(), function);
	}

}
