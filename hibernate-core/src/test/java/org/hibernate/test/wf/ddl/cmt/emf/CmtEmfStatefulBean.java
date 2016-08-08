/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.wf.ddl.cmt.emf;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Arquillian "component" for testing auto-ddl execution when initiated by the "app"
 *
 * @author Steve Ebersole
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class CmtEmfStatefulBean {
	EntityManagerFactory emf;

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void start() {
		// creating the SF should run schema creation
		emf = Persistence.createEntityManagerFactory( "pu-wf-ddl" );
	}

	@Remove
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void stop() {
		// closing the SF should run the delayed schema drop delegate
		emf.close();
	}
}
