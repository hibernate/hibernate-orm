/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.wf.ddl.cmt.sf;

/**
 * @author Andrea Boriero
 */

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.hibernate.test.wf.ddl.WildFlyDdlEntity;


@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CmtSfStatefulBean {

	private static SessionFactory sessionFactory;

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void start() {
		try {
			Configuration configuration = new Configuration();
			configuration = configuration.configure( "hibernate.cfg.xml" );
			configuration.addAnnotatedClass( WildFlyDdlEntity.class );

			sessionFactory = configuration.buildSessionFactory();
		}
		catch (Throwable ex) {
			System.err.println( "Initial SessionFactory creation failed." + ex );
			throw new ExceptionInInitializerError( ex );
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void stop() {
		sessionFactory.close();
	}
}
