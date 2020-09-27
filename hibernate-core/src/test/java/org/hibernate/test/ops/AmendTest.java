/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import static org.junit.Assert.assertEquals;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14228")
public class AmendTest {

	@Test
	public void testNoUpdateAfterAmend() throws Exception {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder().applySetting("hibernate.hbm2ddl.auto",
				"create-drop");
		Metadata metadata = new MetadataSources(srb.build()).addAnnotatedClass(Article.class).buildMetadata();
		SessionFactory sf = metadata.buildSessionFactory();
		EventListenerRegistry registry = ((SessionFactoryImplementor) sf).getServiceRegistry()
				.getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.PRE_UPDATE, new PreUpdateEventListener() {
			@Override
			public boolean onPreUpdate(PreUpdateEvent event) {
				throw new RuntimeException("Update operation shouldn't happen");
			}
		});

		try (Session session = sf.openSession()) {
			Transaction tx = session.beginTransaction();
			Article article = new Article();
			session.save(article); // need retrieve generated ID for construct path later
			article.setPath("/v1/article/" + article.getId());
			session.amend(article);
			article.setPath("/v2/article/" + article.getId());
			session.amend(article);
			tx.commit();
		}

		try (Session session = sf.openSession()) {
			Article article = session.get(Article.class, 1L);
			assertEquals("/v2/article/" + article.getId(), article.getPath());
		}
	}

	@Entity
	@Table(name = "article")
	public static class Article {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String path;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

}
