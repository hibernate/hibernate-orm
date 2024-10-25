/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;


import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.metamodel.mapping.internal.UnifiedAnyDiscriminatorConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class AnyDiscriminatorValueStrategyTests {
	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyImplicitMappingModel(SessionFactoryScope sessions) {
		sessions.withSessionFactory( (factory) -> {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( Order.class );
			final DiscriminatedAssociationAttributeMapping implicitMapping = (DiscriminatedAssociationAttributeMapping) entityDescriptor.findAttributeMapping( "implicitPayment" );
			final DiscriminatorMapping discriminatorMapping = implicitMapping.getDiscriminatorMapping();
			final DiscriminatorConverter<?, ?> discriminatorConverter = discriminatorMapping.getValueConverter();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check discriminator -> entity

			final DiscriminatorValueDetails cash = discriminatorConverter.getDetailsForDiscriminatorValue( CashPayment.class.getName() );
			assertThat( cash.getIndicatedEntity().getEntityName() ).isEqualTo( CashPayment.class.getName() );
			assertThat( cash.getIndicatedEntityName() ).isEqualTo( CashPayment.class.getName() );

			final DiscriminatorValueDetails card = discriminatorConverter.getDetailsForDiscriminatorValue( CardPayment.class.getName() );
			assertThat( card.getIndicatedEntity().getEntityName() ).isEqualTo( CardPayment.class.getName() );
			assertThat( card.getIndicatedEntityName() ).isEqualTo( CardPayment.class.getName() );

			final DiscriminatorValueDetails check = discriminatorConverter.getDetailsForDiscriminatorValue( CheckPayment.class.getName() );
			assertThat( check.getIndicatedEntity().getEntityName() ).isEqualTo( CheckPayment.class.getName() );
			assertThat( check.getIndicatedEntityName() ).isEqualTo( CheckPayment.class.getName() );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check entity -> discriminator

			final DiscriminatorValueDetails cashDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CashPayment.class.getName() );
			assertThat( cashDiscriminatorValue.getValue() ).isEqualTo( CashPayment.class.getName() );

			final DiscriminatorValueDetails cardDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CardPayment.class.getName() );
			assertThat( cardDiscriminatorValue.getValue() ).isEqualTo( CardPayment.class.getName() );

			final DiscriminatorValueDetails checkDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CheckPayment.class.getName() );
			assertThat( checkDiscriminatorValue.getValue() ).isEqualTo( CheckPayment.class.getName() );

			final Map<String,?> detailsByEntityName = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByEntityName();
			assertThat( detailsByEntityName.keySet() ).containsOnly(
					CashPayment.class.getName(),
					CardPayment.class.getName(),
					CheckPayment.class.getName()
			);

			final Map<Object,?> detailsByValue = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByValue();
			assertThat( detailsByValue.keySet() ).containsOnly(
					CashPayment.class.getName(),
					CardPayment.class.getName(),
					CheckPayment.class.getName()
			);
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyExplicitMappingModel(SessionFactoryScope sessions) {
		sessions.withSessionFactory( (factory) -> {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( Order.class );
			final DiscriminatedAssociationAttributeMapping explicitMapping = (DiscriminatedAssociationAttributeMapping) entityDescriptor.findAttributeMapping( "explicitPayment" );
			final DiscriminatorMapping discriminatorMapping = explicitMapping.getDiscriminatorMapping();
			final DiscriminatorConverter<?, ?> discriminatorConverter = discriminatorMapping.getValueConverter();

			// NOTE : cash is NOT mapped

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check discriminator -> entity

			try {
				discriminatorConverter.getDetailsForDiscriminatorValue( "CASH" );
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown discriminator value" );
			}

			try {
				discriminatorConverter.getDetailsForDiscriminatorValue( CashPayment.class.getName() );
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown discriminator value" );
			}

			final DiscriminatorValueDetails card = discriminatorConverter.getDetailsForDiscriminatorValue( "CARD" );
			assertThat( card.getIndicatedEntity().getEntityName() ).isEqualTo( CardPayment.class.getName() );
			assertThat( card.getIndicatedEntityName() ).isEqualTo( CardPayment.class.getName() );

			final DiscriminatorValueDetails check = discriminatorConverter.getDetailsForDiscriminatorValue( "CHECK" );
			assertThat( check.getIndicatedEntity().getEntityName() ).isEqualTo( CheckPayment.class.getName() );
			assertThat( check.getIndicatedEntityName() ).isEqualTo( CheckPayment.class.getName() );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check entity -> discriminator

			try {
				discriminatorConverter.getDetailsForDiscriminatorValue( CashPayment.class.getName() );
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown discriminator value" );
			}

			final DiscriminatorValueDetails cardDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CardPayment.class.getName() );
			assertThat( cardDiscriminatorValue.getValue() ).isEqualTo( "CARD" );

			final DiscriminatorValueDetails checkDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CheckPayment.class.getName() );
			assertThat( checkDiscriminatorValue.getValue() ).isEqualTo( "CHECK" );


			final Map<String,?> detailsByEntityName = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByEntityName();
			assertThat( detailsByEntityName.keySet() ).containsOnly(
					CardPayment.class.getName(),
					CheckPayment.class.getName()
			);

			final Map<Object,?> detailsByValue = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByValue();
			assertThat( detailsByValue.keySet() ).containsOnly(
					"CARD",
					"CHECK"
			);
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyMixedMappingModel(SessionFactoryScope sessions) {
		sessions.withSessionFactory( (factory) -> {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( Order.class );
			final DiscriminatedAssociationAttributeMapping mixedMapping = (DiscriminatedAssociationAttributeMapping) entityDescriptor.findAttributeMapping( "mixedPayment" );
			final DiscriminatorMapping discriminatorMapping = mixedMapping.getDiscriminatorMapping();
			final DiscriminatorConverter<?, ?> discriminatorConverter = discriminatorMapping.getValueConverter();

			// NOTE : cash is NOT mapped

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check discriminator -> entity

			final DiscriminatorValueDetails cash = discriminatorConverter.getDetailsForDiscriminatorValue( CashPayment.class.getName() );
			assertThat( cash.getIndicatedEntity().getEntityName() ).isEqualTo( CashPayment.class.getName() );
			assertThat( cash.getIndicatedEntityName() ).isEqualTo( CashPayment.class.getName() );

			final DiscriminatorValueDetails card = discriminatorConverter.getDetailsForDiscriminatorValue( "CARD" );
			assertThat( card.getIndicatedEntity().getEntityName() ).isEqualTo( CardPayment.class.getName() );
			assertThat( card.getIndicatedEntityName() ).isEqualTo( CardPayment.class.getName() );

			final DiscriminatorValueDetails check = discriminatorConverter.getDetailsForDiscriminatorValue( "CHECK" );
			assertThat( check.getIndicatedEntity().getEntityName() ).isEqualTo( CheckPayment.class.getName() );
			assertThat( check.getIndicatedEntityName() ).isEqualTo( CheckPayment.class.getName() );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// check entity -> discriminator

			final DiscriminatorValueDetails cashDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CashPayment.class.getName() );
			assertThat( cashDiscriminatorValue.getValue() ).isEqualTo( CashPayment.class.getName() );

			final DiscriminatorValueDetails cardDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CardPayment.class.getName() );
			assertThat( cardDiscriminatorValue.getValue() ).isEqualTo( "CARD" );

			final DiscriminatorValueDetails checkDiscriminatorValue = discriminatorConverter.getDetailsForEntityName( CheckPayment.class.getName() );
			assertThat( checkDiscriminatorValue.getValue() ).isEqualTo( "CHECK" );

			final Map<String,?> detailsByEntityName = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByEntityName();
			assertThat( detailsByEntityName.keySet() ).containsOnly(
					CashPayment.class.getName(),
					CardPayment.class.getName(),
					CheckPayment.class.getName()
			);

			final Map<Object,?> detailsByValue = ((UnifiedAnyDiscriminatorConverter<?,?>) discriminatorConverter).getDetailsByValue();
			assertThat( detailsByValue.keySet() ).containsOnly(
					CashPayment.class.getName(),
					"CARD",
					"CHECK"
			);
		} );
	}
}
