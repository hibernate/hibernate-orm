/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.actionqueue;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Yanming Zhou
 */

@Jpa(annotatedClasses = SquashUpdateIntoInsertTest.Article.class)
public class SquashUpdateIntoInsertTest {

	@Test
	@JiraKey("HHH-14228")
	public void testUpdateShouldNotHappens(EntityManagerFactoryScope scope) {
		final EventListenerRegistry registry = scope.getEntityManagerFactory()
				.unwrap(SessionFactoryImplementor.class).getServiceRegistry().getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.PRE_UPDATE, event -> {
			throw new RuntimeException("Update operation shouldn't happen");
		});

		final Article article = new Article();
		scope.inTransaction(em -> {
			em.persist(article); // need retrieve generated ID to construct path later
			article.setPath("/article/" + article.getId());
		});
		scope.inTransaction(em -> {
			Article articleInDB = em.find(Article.class, article.getId());
			assertEquals("/article/" + article.getId(), articleInDB.getPath());
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