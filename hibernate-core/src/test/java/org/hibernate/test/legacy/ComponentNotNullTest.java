/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.legacy;
import java.util.ArrayList;

import org.junit.Test;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static org.junit.Assert.fail;

/**
 * Test some cases of not-null properties inside components.
 *
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class ComponentNotNullTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
			"legacy/ComponentNotNullMaster.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Simple.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CHECK_NULLABILITY, "true" );
	}

	@Test
	public void testComponentNotNull() throws Exception {

		//everything not null
		//
		Session s = openSession();
		Transaction t = s.beginTransaction();
		ComponentNotNullMaster master = new ComponentNotNullMaster();
		ComponentNotNull nullable = new ComponentNotNull();
		ComponentNotNull supercomp = new ComponentNotNull();
		ComponentNotNull subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		master.setSupercomp(supercomp);
		s.save(master);
		t.commit();
		s.close();

		//null prop of a subcomp
		//
		s = openSession();
		t = s.beginTransaction();

		master = new ComponentNotNullMaster();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		// do not set property
		//subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		master.setSupercomp(supercomp);


		try {
			s.save(master);
			t.commit();
			fail("Inserting not-null null property should fail");
		} catch (PropertyValueException e) {
			//succeed
		}
		t.rollback();
		s.close();

		//null component having not-null column
		//
		s = openSession();
		t = s.beginTransaction();

		master = new ComponentNotNullMaster();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		// do not set supercomp for master
		//subcomp.setProp1Subcomp("test");
		//supercomp.setSubcomp(subcomp);
		//master.setSupercomp(supercomp);


		try {
			s.save(master);
			t.commit();
			fail("Inserting not-null null property should fail");
		} catch (PropertyValueException e) {
			//succeed
		}
		t.rollback();
		s.close();
	}

	@Test
	public void testCompositeElement() throws Exception {
		//composite-element nullable
		Session s = openSession();
		Transaction t = s.beginTransaction();
		ComponentNotNullMaster master = new ComponentNotNullMaster();
		ComponentNotNull nullable = new ComponentNotNull();
		ComponentNotNull supercomp = new ComponentNotNull();
		ComponentNotNull subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		master.setSupercomp(supercomp);

		master.setComponents(new ArrayList());
		ComponentNotNullMaster.ContainerInnerClass cc =
			new ComponentNotNullMaster.ContainerInnerClass();
		master.getComponents().add(cc);

		try {
			s.save(master);
			t.commit();
			fail("Inserting not-null many-to-one should fail");
		} catch (PropertyValueException e) {
			//success
		}
		t.rollback();
		s.close();

		//null nested component having not-null column
		//
		s = openSession();
		t = s.beginTransaction();

		master = new ComponentNotNullMaster();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		master.setSupercomp(supercomp);

		master.setComponentsImplicit(new ArrayList());
		ComponentNotNullMaster.ContainerInnerClass nestedCc =
			new ComponentNotNullMaster.ContainerInnerClass();
		cc =
			new ComponentNotNullMaster.ContainerInnerClass();
		cc.setNested(nestedCc);
		master.getComponentsImplicit().add(cc);

		try {
			s.save(master);
			t.commit();
			fail("Inserting not-null null property should fail");
		} catch (PropertyValueException e) {
			//succeed
		}
		t.rollback();
		s.close();

		//nested component having not-null column
		//
		s = openSession();
		t = s.beginTransaction();

		master = new ComponentNotNullMaster();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		master.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		master.setSupercomp(supercomp);

		master.setComponentsImplicit(new ArrayList());
		nestedCc =
			new ComponentNotNullMaster.ContainerInnerClass();
		cc =
			new ComponentNotNullMaster.ContainerInnerClass();
		cc.setNested(nestedCc);
		nestedCc.setNestedproperty("test");
		master.getComponentsImplicit().add(cc);

		s.save(master);
		t.commit();
		s.close();
	}

}
