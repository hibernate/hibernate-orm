/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel;

import java.util.Arrays;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 * 
 * @author Hernán Chanfreau
 * 
 *         Same test from package
 *         org.hibernate.envers.test.integration.superclass changing the Audited
 *         annotation in MappedSuperclass from property str to class level
 */
public class MappedSubclassing2 extends AbstractEntityTest {
	private Integer id1;

	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(SubclassEntity2.class);
	}

	@BeforeClass(dependsOnMethods = "init")
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		SubclassEntity2 se1 = new SubclassEntity2("x");
		em.persist(se1);
		id1 = se1.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		se1 = em.find(SubclassEntity2.class, id1);
		se1.setStr("y");
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList(1, 2).equals(
				getAuditReader().getRevisions(SubclassEntity2.class, id1));
	}

	@Test
	public void testHistoryOfId1() {
		SubclassEntity2 ver1 = new SubclassEntity2(id1, "x");
		SubclassEntity2 ver2 = new SubclassEntity2(id1, "y");

		assert getAuditReader().find(SubclassEntity2.class, id1, 1)
				.equals(ver1);
		assert getAuditReader().find(SubclassEntity2.class, id1, 2)
				.equals(ver2);
	}
}
