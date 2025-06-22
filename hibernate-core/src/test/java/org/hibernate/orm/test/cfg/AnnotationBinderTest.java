/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static junit.framework.TestCase.fail;

/**
 * @author Dominique Toupin
 */
@JiraKey(value = "HHH-10456")
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
