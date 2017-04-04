/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.pk;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Gail Badner
 */
public class EmbeddedPKTestTask extends AbstractEnhancerTestTask {

	private Long entityId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {WorkOrder.class, WorkOrderPK.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		WorkOrder wo = new WorkOrder(  );
		s.persist( wo );

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	public void execute() {

	}

	protected void cleanup() {
	}

}
