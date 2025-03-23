/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@JiraKey("HHH-17908")
@RequiresDialect( H2Dialect.class )
@RequiresDialect( MySQLDialect.class )
public class ExistingVarcharEnumColumnValidationTest extends BaseCoreFunctionalTestCase {

	private StandardServiceRegistry ssr;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityE.class };
	}

	@Before
	public void setUp() {
		try {
			tearDown();
		}
		catch (Exception ex) {
			// ignore
		}
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery(
							"create table en (id integer not null, sign_position varchar(255) check (sign_position in ('AFTER_NO_SPACE','AFTER_WITH_SPACE','BEFORE_NO_SPACE','BEFORE_WITH_SPACE')), primary key (id))" )
					.executeUpdate();
		} );
	}

	@After
	public void tearDown() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "drop table en cascade" ).executeUpdate();
		} );
	}

	@Test
	public void testEnumDataTypeSchemaValidator() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "validate" )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( EntityE.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "en")
	@Table(name = "en")
	public static class EntityE {
		@Id
		@Column(name = "id", nullable = false, updatable = false)
		private Integer id;

		@Enumerated(EnumType.STRING)
		@Column(name = "sign_position")
		private SignPosition signPosition;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SignPosition getSignPosition() {
			return signPosition;
		}

		public void setSignPosition(SignPosition signPosition) {
			this.signPosition = signPosition;
		}
	}

	public enum SignPosition {
		AFTER_NO_SPACE, AFTER_WITH_SPACE, BEFORE_NO_SPACE, BEFORE_WITH_SPACE
	}
}
