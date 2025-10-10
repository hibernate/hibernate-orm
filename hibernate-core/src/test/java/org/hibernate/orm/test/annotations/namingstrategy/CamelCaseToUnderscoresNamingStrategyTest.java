/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test harness for HHH-17310.
 *
 * @author Anilabha Baral
 */
@BaseUnitTest
public class CamelCaseToUnderscoresNamingStrategyTest {

	private ServiceRegistry serviceRegistry;

	@BeforeAll
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterAll
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithWordWithDigitNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( B.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new PhysicalNamingStrategySnakeCaseImpl() )
				.build();

		PersistentClass entityBinding = metadata.getEntityBinding( B.class.getName() );
		assertThat( entityBinding.getProperty( "wordWithDigitD1" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "word_with_digit_d1" );
		assertThat( entityBinding.getProperty( "AbcdEfghI21" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "abcd_efgh_i21" );
		assertThat( entityBinding.getProperty( "hello1" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "hello1" );
		assertThat( entityBinding.getProperty( "hello1D2" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "hello1_d2" );
		assertThat( entityBinding.getProperty( "hello3d4" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "hello3d4" );
		assertThat( entityBinding.getProperty( "quoted" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "Quoted-ColumnName" );
	}

	@Entity
	@Table(name = "ABCD")
	class B implements java.io.Serializable {
		@Id
		protected String AbcdEfghI21;
		protected String wordWithDigitD1;
		protected String hello1;
		protected String hello1D2;
		protected String hello3d4;
		@Column(name = "\"Quoted-ColumnName\"")
		protected String quoted;

		public String getAbcdEfghI21() {
			return AbcdEfghI21;
		}

		public void setAbcdEfghI21(String abcdEfghI21) {
			AbcdEfghI21 = abcdEfghI21;
		}

		public String getWordWithDigitD1() {
			return wordWithDigitD1;
		}

		public void setWordWithDigitD1(String wordWithDigitD1) {
			this.wordWithDigitD1 = wordWithDigitD1;
		}

		public String getHello1() {
			return hello1;
		}

		public void setHello1(String hello1) {
			this.hello1 = hello1;
		}

		public String getHello1D2() {
			return hello1D2;
		}

		public void setHello1D2(String hello1D2) {
			this.hello1D2 = hello1D2;
		}

		public String getHello3d4() {
			return hello3d4;
		}

		public void setHello3d4(String hello3d4) {
			this.hello3d4 = hello3d4;
		}
	}
}
