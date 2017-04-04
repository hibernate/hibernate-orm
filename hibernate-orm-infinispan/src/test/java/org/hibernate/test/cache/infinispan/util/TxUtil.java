package org.hibernate.test.cache.infinispan.util;

import java.util.concurrent.Callable;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public final class TxUtil {
	public static void withTxSession(boolean useJta, SessionFactory sessionFactory, ThrowingConsumer<Session, Exception> consumer) throws Exception {
		JtaPlatform jtaPlatform = useJta ? sessionFactory.getSessionFactoryOptions().getServiceRegistry().getService(JtaPlatform.class) : null;
		withTxSession(jtaPlatform, sessionFactory.withOptions(), consumer);
	}

	public static void withTxSession(JtaPlatform jtaPlatform, SessionBuilder sessionBuilder, ThrowingConsumer<Session, Exception> consumer) throws Exception {
		if (jtaPlatform != null) {
			TransactionManager tm = jtaPlatform.retrieveTransactionManager();
			final SessionBuilder sb = sessionBuilder;
			Caches.withinTx(tm, () -> {
				withSession(sb, s -> {
					consumer.accept(s);
					// we need to flush the session before close when running with JTA transactions
					s.flush();
				});
				return null;
			});
		} else {
			withSession(sessionBuilder, s -> withResourceLocalTx(s, consumer));
		}
	}

	public static <T> T withTxSessionApply(boolean useJta, SessionFactory sessionFactory, ThrowingFunction<Session, T, Exception> function) throws Exception {
		JtaPlatform jtaPlatform = useJta ? sessionFactory.getSessionFactoryOptions().getServiceRegistry().getService(JtaPlatform.class) : null;
		return withTxSessionApply(jtaPlatform, sessionFactory.withOptions(), function);
	}

	public static <T> T withTxSessionApply(JtaPlatform jtaPlatform, SessionBuilder sessionBuilder, ThrowingFunction<Session, T, Exception> function) throws Exception {
		if (jtaPlatform != null) {
			TransactionManager tm = jtaPlatform.retrieveTransactionManager();
			Callable<T> callable = () -> withSessionApply(sessionBuilder, s -> {
				T t = function.apply(s);
				s.flush();
				return t;
			});
			return Caches.withinTx(tm, callable);
		} else {
			return withSessionApply(sessionBuilder, s -> withResourceLocalTx(s, function));
		}
	}

	public static <E extends Throwable> void withSession(SessionBuilder sessionBuilder, ThrowingConsumer<Session, E> consumer) throws E {
		Session s = sessionBuilder.openSession();
		try {
			consumer.accept(s);
		} finally {
			s.close();
		}
	}

	public static <R, E extends Throwable> R withSessionApply(SessionBuilder sessionBuilder, ThrowingFunction<Session, R, E> function) throws E {
		Session s = sessionBuilder.openSession();
		try {
			return function.apply(s);
		} finally {
			s.close();
		}
	}

	public static void withResourceLocalTx(Session session, ThrowingConsumer<Session, Exception> consumer) throws Exception {
		Transaction transaction = session.beginTransaction();
		boolean rollingBack = false;
		try {
			consumer.accept(session);
			if (transaction.getStatus() == TransactionStatus.ACTIVE) {
				transaction.commit();
			} else {
				rollingBack = true;
				transaction.rollback();
			}
		} catch (Exception e) {
			if (!rollingBack) {
				try {
					transaction.rollback();
				} catch (Exception suppressed) {
					e.addSuppressed(suppressed);
				}
			}
			throw e;
		}
	}

	public static <T> T withResourceLocalTx(Session session, ThrowingFunction<Session, T, Exception> consumer) throws Exception {
		Transaction transaction = session.beginTransaction();
		boolean rollingBack = false;
		try {
			T t = consumer.apply(session);
			if (transaction.getStatus() == TransactionStatus.ACTIVE) {
				transaction.commit();
			} else {
				rollingBack = true;
				transaction.rollback();
			}
			return t;
		} catch (Exception e) {
			if (!rollingBack) {
				try {
					transaction.rollback();
				} catch (Exception suppressed) {
					e.addSuppressed(suppressed);
				}
			}
			throw e;
		}
	}

	public static void markRollbackOnly(boolean useJta, Session s) {
		if (useJta) {
			JtaPlatform jtaPlatform = s.getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService(JtaPlatform.class);
			TransactionManager tm = jtaPlatform.retrieveTransactionManager();
			try {
				tm.setRollbackOnly();
			} catch (SystemException e) {
				throw new RuntimeException(e);
			}
		} else {
			s.getTransaction().markRollbackOnly();
		}
	}

	public interface ThrowingConsumer<T, E extends Throwable> {
		void accept(T t) throws E;
	}

	public interface ThrowingFunction<T, R, E extends Throwable> {
		R apply(T t) throws E;
	}
}
