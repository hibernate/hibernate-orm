/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


/**
 * @author Yanming Zhou
 */
@Exclude
public class CompositeIdTypeMismatchTest {

	@Test
	public void test() {
		try ( StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder().build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntity.class );
			assertThatExceptionOfType( AnnotationException.class).isThrownBy( metadataSources::buildMetadata ).withMessageContaining( "doesn't match type" );
		}
	}

	@IdClass(TestEntity.ID.class)
	@Entity
	static class TestEntity {

		@Id
		String code;


		@Id
		Integer type;

		record ID(String code, String type) {

		}
	}
}
