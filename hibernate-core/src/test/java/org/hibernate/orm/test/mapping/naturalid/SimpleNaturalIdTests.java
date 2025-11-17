/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.money.Monetary;

import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.Vendor;
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
 * Tests for simple (single attribute) natural-ids
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class SimpleNaturalIdTests {
	private static final UUID uuid = SafeRandomUUIDGenerator.safeRandomUUID();

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Vendor vendor = new Vendor( 1, "Acme Brick", "Acme Global" );
					session.persist( vendor );

					final Product product = new Product(
							1,
							uuid,
							vendor,
							Monetary.getDefaultAmountFactory().setNumber( 1L ).setCurrency( Monetary.getCurrency( Locale.US ) ).create()
					);
					session.persist( product );
				}
		);
	}

	@AfterEach
	public void releaseTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testProcessing(DomainModelScope domainModelScope, SessionFactoryScope factoryScope) {
		final PersistentClass productBootMapping = domainModelScope.getDomainModel().getEntityBinding( Product.class.getName() );
		assertThat( productBootMapping.hasNaturalId(), is( true ) );
		final Property sku = productBootMapping.getProperty( "sku" );
		assertThat( sku.isNaturalIdentifier(), is( true ) );

		final MappingMetamodel mappingMetamodel = factoryScope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister productMapping = mappingMetamodel.findEntityDescriptor( Product.class );
		assertThat( productMapping.hasNaturalIdentifier(), is( true ) );
		final NaturalIdMapping naturalIdMapping = productMapping.getNaturalIdMapping();
		assertThat( naturalIdMapping, notNullValue() );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SimpleNaturalIdLoadAccess<Product> loadAccess = session.bySimpleNaturalId( Product.class );
					verifyEntity( loadAccess.getReference( uuid ) );
				}
		);
	}

	public void verifyEntity(Product productRef) {
		assertThat( productRef, notNullValue() );
		assertThat( productRef.getId(), is( 1 ) );
		assertThat( productRef.getSku(), is( uuid ) );
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SimpleNaturalIdLoadAccess<Product> loadAccess = session.bySimpleNaturalId( Product.class );
					verifyEntity( loadAccess.load( uuid ) );
				}
		);
	}

	@Test
	public void testOptionalLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SimpleNaturalIdLoadAccess<Product> loadAccess = session.bySimpleNaturalId( Product.class );
					final Optional<Product> optionalProduct = loadAccess.loadOptional( uuid );
					assertThat( optionalProduct.isPresent(), is( true ) );
					verifyEntity( optionalProduct.get() );
				}
		);
	}

	@Test
	public void testMultiLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final NaturalIdMultiLoadAccess<Product> loadAccess = session.byMultipleNaturalId( Product.class );
					loadAccess.enableOrderedReturn( false );
					final List<Product> products = loadAccess.multiLoad( uuid );
					assertThat( products.size(), is( 1 ) );
					verifyEntity( products.get( 0 ) );
				}
		);
	}
}
