/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cfg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static junit.framework.TestCase.fail;

/**
 * @author Dominique Toupin
 */
@TestForIssue(jiraKey = "HHH-10456")
public class AnnotationBinderTest {

	@Test
	public void testInvalidPrimaryKeyJoinColumn() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			try {
				new MetadataSources( serviceRegistry )
						.addAnnotatedClass( InvalidPrimaryKeyJoinColumnAnnotationEntity.class )
						.buildMetadata();
				fail();
			}
			catch (AnnotationException ae) {
				// expected!
			}
		}
	}

	@Entity
	@PrimaryKeyJoinColumn
	public static class InvalidPrimaryKeyJoinColumnAnnotationEntity {

		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

}
