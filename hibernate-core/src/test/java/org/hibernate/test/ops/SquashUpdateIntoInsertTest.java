/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14228")
public class SquashUpdateIntoInsertTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Article.class };
	}

	@Before
	public void SetUp() {
		EventListenerRegistry registry = sessionFactory().getServiceRegistry().getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.PRE_UPDATE, new PreUpdateEventListener() {
			@Override
			public boolean onPreUpdate(PreUpdateEvent event) {
				throw new RuntimeException("Update operation shouldn't happen");
			}
		});
	}

	@Test
	public void testUpdateShouldNotHappens() {
		Article article = new Article();
		doInHibernate(this::sessionFactory, session -> {
			session.save(article); // need retrieve generated ID for construct path later
			article.setPath("/v1/article/" + article.getId());
			article.setPath("/v2/article/" + article.getId());
		});
		doInHibernate(this::sessionFactory, session -> {
			Article articleInDB = session.find(Article.class, article.getId());
			assertEquals("/v2/article/" + articleInDB.getId(), articleInDB.getPath());
		});
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
