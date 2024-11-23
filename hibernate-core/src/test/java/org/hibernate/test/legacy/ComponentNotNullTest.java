/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.util.ArrayList;

import org.junit.Test;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import static org.junit.Assert.fail;

/**
 * Test some cases of not-null properties inside components.
 *
 * @author Emmanuel Bernard
 */
public class ComponentNotNullTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
			"legacy/ComponentNotNullRoot.hbm.xml",
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
		ComponentNotNullRoot root = new ComponentNotNullRoot();
		ComponentNotNull nullable = new ComponentNotNull();
		ComponentNotNull supercomp = new ComponentNotNull();
		ComponentNotNull subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		root.setSupercomp(supercomp);
		s.save(root);
		t.commit();
		s.close();

		//null prop of a subcomp
		//
		s = openSession();
		t = s.beginTransaction();

		root = new ComponentNotNullRoot();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		// do not set property
		//subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		root.setSupercomp(supercomp);


		try {
			s.save(root);
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

		root = new ComponentNotNullRoot();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		// do not set supercomp for root
		//subcomp.setProp1Subcomp("test");
		//supercomp.setSubcomp(subcomp);
		//root.setSupercomp(supercomp);


		try {
			s.save(root);
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
		ComponentNotNullRoot root = new ComponentNotNullRoot();
		ComponentNotNull nullable = new ComponentNotNull();
		ComponentNotNull supercomp = new ComponentNotNull();
		ComponentNotNull subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		root.setSupercomp(supercomp);

		root.setComponents(new ArrayList());
		ComponentNotNullRoot.ContainerInnerClass cc =
			new ComponentNotNullRoot.ContainerInnerClass();
		root.getComponents().add(cc);

		try {
			s.save(root);
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

		root = new ComponentNotNullRoot();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		root.setSupercomp(supercomp);

		root.setComponentsImplicit(new ArrayList());
		ComponentNotNullRoot.ContainerInnerClass nestedCc =
			new ComponentNotNullRoot.ContainerInnerClass();
		cc =
			new ComponentNotNullRoot.ContainerInnerClass();
		cc.setNested(nestedCc);
		root.getComponentsImplicit().add(cc);

		try {
			s.save(root);
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

		root = new ComponentNotNullRoot();
		nullable = new ComponentNotNull();
		supercomp = new ComponentNotNull();
		subcomp = new ComponentNotNull();

		root.setNullable(nullable);
		subcomp.setProp1Subcomp("test");
		supercomp.setSubcomp(subcomp);
		root.setSupercomp(supercomp);

		root.setComponentsImplicit(new ArrayList());
		nestedCc =
			new ComponentNotNullRoot.ContainerInnerClass();
		cc =
			new ComponentNotNullRoot.ContainerInnerClass();
		cc.setNested(nestedCc);
		nestedCc.setNestedproperty("test");
		root.getComponentsImplicit().add(cc);

		s.save(root);
		t.commit();
		s.close();
	}

}
