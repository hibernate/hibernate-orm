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
package org.hibernate.envers.test.integration.hashcode;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChangingHashcode extends AbstractEntityTest {
	private Long pageId;
	private Long imageId;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(WikiPage.class);
        cfg.addAnnotatedClass(WikiImage.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

		WikiPage page = new WikiPage("title", "content");
        em.persist(page);

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        WikiImage image = new WikiImage("name1");
		em.persist(image);

		page = em.find(WikiPage.class, page.getId());
		page.getImages().add(image);

        em.getTransaction().commit();

        // Revision 3
        em = getEntityManager();
        em.getTransaction().begin();

        image = em.find(WikiImage.class, image.getId());
		image.setName("name2");

        em.getTransaction().commit();

		pageId = page.getId();
		imageId = image.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(WikiPage.class, pageId));
        assert Arrays.asList(2, 3).equals(getAuditReader().getRevisions(WikiImage.class, imageId));
    }

    @Test
    public void testHistoryOfImage() {
		assert getAuditReader().find(WikiImage.class, imageId, 1) == null;
        assert getAuditReader().find(WikiImage.class, imageId, 2).equals(new WikiImage("name1"));
        assert getAuditReader().find(WikiImage.class, imageId, 3).equals(new WikiImage("name2"));
    }

    @Test
    public void testHistoryOfPage() {
        assert getAuditReader().find(WikiPage.class, pageId, 1).getImages().size() == 0;
        assert getAuditReader().find(WikiPage.class, pageId, 2).getImages().equals(TestTools.makeSet(new WikiImage("name1")));
        assert getAuditReader().find(WikiPage.class, pageId, 3).getImages().equals(TestTools.makeSet(new WikiImage("name2")));
    }
}