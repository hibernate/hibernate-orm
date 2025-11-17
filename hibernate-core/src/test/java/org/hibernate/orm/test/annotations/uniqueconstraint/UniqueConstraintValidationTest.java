/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Nikolay Shestakov
 *
 */
@BaseUnitTest
public class UniqueConstraintValidationTest {

	@Test
	@JiraKey(value = "HHH-4084")
	public void testUniqueConstraintWithEmptyColumnName() {
		assertThrows( AnnotationException.class, () ->
				buildSessionFactory( EmptyColumnNameEntity.class )
		);
	}

	@Test
	public void testUniqueConstraintWithEmptyColumnNameList() {
		assertThrows( AnnotationException.class, () ->
				buildSessionFactory( EmptyColumnNameListEntity.class )
		);
	}

	@Test
	public void testUniqueConstraintWithNotExistsColumnName() {
		buildSessionFactory(NotExistsColumnEntity.class);
	}

	private void buildSessionFactory(Class<?> entity) {
		StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();

		try {
			new MetadataSources( serviceRegistry )
					.addAnnotatedClass( entity )
					.buildMetadata()
					.buildSessionFactory()
					.close();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Entity
	@Table(name = "tbl_emptycolumnnameentity", uniqueConstraints = @UniqueConstraint(columnNames = ""))
	public static class EmptyColumnNameEntity implements Serializable {
		@Id
		protected Long id;
	}

	@Entity
	@Table(name = "tbl_emptycolumnnamelistentity", uniqueConstraints = @UniqueConstraint(columnNames = {}))
	public static class EmptyColumnNameListEntity implements Serializable {
		@Id
		protected Long id;
	}

	@Entity
	@Table(name = "tbl_notexistscolumnentity", uniqueConstraints = @UniqueConstraint(columnNames = "notExists"))
	public static class NotExistsColumnEntity implements Serializable {
		@Id
		protected Long id;
	}
}
