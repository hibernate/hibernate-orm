/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy.charset;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.orm.test.annotations.namingstrategy.LongIdentifierNamingStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12357")
@BaseUnitTest
public abstract class AbstractCharsetNamingStrategyTest {

	protected ServiceRegistry serviceRegistry;

	@BeforeAll
	public void setUp() {
		Map<String, Object> properties = PropertiesHelper.map( Environment.getProperties() );
		properties.put( AvailableSettings.HBM2DDL_CHARSET_NAME, charsetName() );
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( properties );
	}

	protected abstract String charsetName();

	@AfterAll
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new LongIdentifierNamingStrategy() )
				.build();

		var uniqueKey = metadata.getEntityBinding( Address.class.getName() ).getTable().getUniqueKeys().values()
				.iterator().next();
		assertThat( uniqueKey.getName() ).isEqualTo( expectedUniqueKeyName() );

		var foreignKey = metadata.getEntityBinding( Address.class.getName() ).getTable().getForeignKeyCollection()
				.iterator().next();
		assertThat( foreignKey.getName() ).isEqualTo( expectedForeignKeyName() );

		var index = metadata.getEntityBinding( Address.class.getName() ).getTable().getIndexes().values().iterator()
				.next();
		assertThat( index.getName() ).isEqualTo( expectedIndexName() );
	}

	protected abstract String expectedUniqueKeyName();

	protected abstract String expectedForeignKeyName();

	protected abstract String expectedIndexName();

	@Entity(name = "Address")
	@Table(uniqueConstraints = @UniqueConstraint(
			columnNames = {
					"city", "stradă"
			}),
			indexes = @Index(columnList = "city, stradă")
	)
	public class Address {

		@Id
		private Long id;

		private String city;

		private String stradă;

		@ManyToOne
		private Person personă;
	}

	@Entity(name = "Person")
	public class Person {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
