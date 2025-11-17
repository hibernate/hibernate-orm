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
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dominique Toupin
 */
@JiraKey(value = "HHH-10456")
@BaseUnitTest
public class AnnotationBinderTest {

	@Test
	public void testInvalidPrimaryKeyJoinColumn() {
		AnnotationException annotationException = assertThrows( AnnotationException.class, () -> {
			try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
				new MetadataSources( serviceRegistry )
						.addAnnotatedClass( InvalidPrimaryKeyJoinColumnAnnotationEntity.class )
						.buildMetadata();
			}
		} );
		assertThat( annotationException.getMessage() ).contains( "InvalidPrimaryKeyJoinColumnAnnotationEntity" );
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
