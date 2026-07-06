/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.YesNoConverter;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class ValidationTests {
	@Test
	void testLazyToOne() {
		try (var serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			final Metadata metadata = MetadataBuildingTestHelper.buildMetadata(
					serviceRegistry,
					new MappingSources()
							.addManagedClass( Person.class )
							.addManagedClass( Address.class )
			);
			try (SessionFactory sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
				fail( "Expecting a failure" );
			}
			catch (UnsupportedMappingException expected) {
			}
		}
	}

	@Test
	void testCustomSql() {
		try (var serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			final Metadata metadata = MetadataBuildingTestHelper.buildMetadata(
					serviceRegistry,
					new MappingSources().addManagedClass( NoNo.class )
			);
			try (SessionFactory sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
				fail( "Expecting a failure" );
			}
			catch (UnsupportedMappingException expected) {
			}
		}
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="address_fk")
		private Address address;
	}

	@Entity(name="Address")
	@Table(name="addresses")
	@SoftDelete(converter = YesNoConverter.class, strategy = SoftDeleteType.ACTIVE)
	public static class Address {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="NoNo")
	@Table(name="nonos")
	@SoftDelete(converter = YesNoConverter.class, strategy = SoftDeleteType.ACTIVE)
	@SQLDelete( sql = "delete from nonos" )
	public static class NoNo {
		@Id
		private Integer id;
		private String name;
	}
}
