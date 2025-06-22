/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for composite (multiple attributes) natural-ids
 */
@DomainModel( annotatedClasses = CompoundNaturalIdTests.Account.class )
@SessionFactory
public class CompoundNaturalIdTests {
	public static final Object[] VALUE_ARRAY = new Object[] { "matrix", "neo" };
	public static final Map<String,String> VALUE_MAP = toMap( "system", "matrix", "username", "neo" );

	private static Map<String, String> toMap(String... values) {
		assert values.length % 2 == 0;

		final HashMap<String,String> valuesMap = new HashMap<>();
		for ( int i = 0; i < values.length; i += 2 ) {
			valuesMap.put( values[i], values[i+1] );
		}

		return valuesMap;
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Account( 1, "neo", "matrix", "neo@nebuchadnezzar.zion.net" ) );
					session.persist( new Account( 2, "trinity", "matrix", "trin@nebuchadnezzar.zion.net" ) );
				}
		);
	}

	@AfterEach
	public void releaseTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testProcessing(DomainModelScope domainModelScope, SessionFactoryScope factoryScope) {
		final PersistentClass accountBootMapping = domainModelScope.getDomainModel().getEntityBinding( Account.class.getName() );
		assertThat( accountBootMapping.hasNaturalId(), is( true ) );
		final Property username = accountBootMapping.getProperty( "username" );
		assertThat( username.isNaturalIdentifier(), is( true ) );
		final Property system = accountBootMapping.getProperty( "system" );
		assertThat( system.isNaturalIdentifier(), is( true ) );

		final MappingMetamodel mappingMetamodel = factoryScope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister accountMapping = mappingMetamodel.findEntityDescriptor( Account.class );
		assertThat( accountMapping.hasNaturalIdentifier(), is( true ) );
		final NaturalIdMapping naturalIdMapping = accountMapping.getNaturalIdMapping();
		assertThat( naturalIdMapping, notNullValue() );

		final List<SingularAttributeMapping> attributes = naturalIdMapping.getNaturalIdAttributes();
		assertThat( attributes.size(), is( 2 ) );

		// alphabetical matching overall processing

		final SingularAttributeMapping first = attributes.get( 0 );
		assertThat( first, notNullValue() );
		assertThat( first.getAttributeName(), is( "system" ) );

		final SingularAttributeMapping second = attributes.get( 1 );
		assertThat( second, notNullValue() );
		assertThat( second.getAttributeName(), is( "username" ) );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final NaturalIdLoadAccess<Account> loadAccess = session.byNaturalId( Account.class );
					loadAccess.using( "system", "matrix" );
					loadAccess.using( "username", "neo" );
					verifyEntity( loadAccess.getReference() );
				}
		);

		scope.inTransaction(
				session -> {
					final MappingMetamodel mappingMetamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
					final EntityPersister accountMapping = mappingMetamodel.findEntityDescriptor( Account.class );
					final NaturalIdMapping naturalIdMapping = accountMapping.getNaturalIdMapping();

					// test load by array
					Object id = accountMapping.getNaturalIdLoader().resolveNaturalIdToId( VALUE_ARRAY, session );
					assertThat( id, is( 1 ) );

					// and by Map
					id = accountMapping.getNaturalIdLoader().resolveNaturalIdToId( VALUE_MAP, session );
					assertThat( id, is( 1 ) );
				}
		);
	}

	public void verifyEntity(Account accountRef) {
		assertThat( accountRef, notNullValue() );
		assertThat( accountRef.getId(), is( 1 ) );
		assertThat( accountRef.getSystem(), is( "matrix" ) );
		assertThat( accountRef.getUsername(), is( "neo" ) );
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Account account = session.byNaturalId( Account.class )
							.using( "system", "matrix" )
							.using( "username", "neo" )
							.load();
					verifyEntity( account );
				}
		);

		scope.inTransaction(
				session -> {
					final MappingMetamodel mappingMetamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
					final EntityPersister accountMapping = mappingMetamodel.findEntityDescriptor( Account.class );
					final NaturalIdMapping naturalIdMapping = accountMapping.getNaturalIdMapping();

					// test load by array
					accountMapping.getNaturalIdLoader().load( VALUE_ARRAY, NaturalIdLoadOptions.NONE, session );

					// and by Map
					accountMapping.getNaturalIdLoader().load( VALUE_MAP, NaturalIdLoadOptions.NONE, session );
				}
		);
	}

	@Test
	public void testOptionalLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final NaturalIdLoadAccess<Account> loadAccess = session.byNaturalId( Account.class );
					final Optional<Account> optionalAccount = loadAccess
							.using( "system", "matrix" )
							.using( "username", "neo" )
							.loadOptional();
					assertThat( optionalAccount.isPresent(), is( true ) );
					verifyEntity( optionalAccount.get() );
				}
		);
	}

	@Test
	public void testMultiLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final NaturalIdMultiLoadAccess<Account> loadAccess = session.byMultipleNaturalId( Account.class );
					loadAccess.enableOrderedReturn( false );
					final List<Account> accounts = loadAccess.multiLoad(
							Map.of( "system", "matrix", "username", "neo" ),
							Map.of( "system", "matrix", "username", "trinity" )
					);
					assertThat( accounts.size(), is( 2 ) );

					final List<Account> byMap = loadAccess.multiLoad( VALUE_MAP );
					assertThat( byMap.size(), is( 1 ) );

					final List<Account> byArray = loadAccess.multiLoad( new Object[] { VALUE_ARRAY } );
					assertThat( byArray.size(), is( 1 ) );
				}
		);
	}


	@Entity( name = "Account" )
	@Table( name = "acct" )
	public static class Account {
		@Id
		private Integer id;
		@NaturalId
		private String username;
		@NaturalId
		@Column(name = "system_name")
		private String system;
		private String emailAddress;

		public Account() {
		}

		public Account(Integer id, String username, String system) {
			this.id = id;
			this.username = username;
			this.system = system;
		}

		public Account(Integer id, String username, String system, String emailAddress) {
			this.id = id;
			this.username = username;
			this.system = system;
			this.emailAddress = emailAddress;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getSystem() {
			return system;
		}

		public void setSystem(String system) {
			this.system = system;
		}

		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
		}
	}
}
