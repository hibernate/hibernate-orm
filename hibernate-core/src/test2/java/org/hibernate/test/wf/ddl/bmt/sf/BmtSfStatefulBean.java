/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.wf.ddl.bmt.sf;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.hibernate.test.wf.ddl.WildFlyDdlEntity;

/**
 * Arquillian "component" for testing auto-ddl execution when initiated by the "app"
 *
 * @author Steve Ebersole
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtSfStatefulBean {
	private static SessionFactory sessionFactory;

	@Inject
	UserTransaction utx;

	public void start() {
		Configuration configuration = new Configuration();
		configuration = configuration.configure( "hibernate.cfg.xml" );
		configuration.addAnnotatedClass( WildFlyDdlEntity.class );

		// creating the SF should run schema creation
		sessionFactory = configuration.buildSessionFactory();
	}

	@Remove
	public void stop() {
		try {
			utx.begin();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to start JTA transaction via UserTransaction", e );
		}

		try {
			// closing the SF should run the delayed schema drop delegate
			sessionFactory.close();
		}
		catch (RuntimeException e) {
			try {
				utx.rollback();
			}
			catch (Exception e1) {
				throw new RuntimeException( "Unable to rollback JTA transaction via UserTransaction", e );
			}

			throw e;
		}

		try {
			utx.commit();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to commit JTA transaction via UserTransaction", e );
		}
	}
}
