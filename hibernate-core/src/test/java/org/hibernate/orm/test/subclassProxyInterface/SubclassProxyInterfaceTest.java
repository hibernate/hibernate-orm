/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassProxyInterface;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BaseUnitTest
@RequiresDialect(
		value = H2Dialect.class,
		comment = "This is a true unit test.  The Dialect is irrelevant."
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/subclassProxyInterface/Person.hbm.xml" )
public class SubclassProxyInterfaceTest {
	@Test
	public void testSubclassProxyInterfaces(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();

		final PersistentClass entityBinding = domainModel.getEntityBinding( Doctor.class.getName() );
		assertThat( entityBinding.getProxyInterfaceName() ).isEqualTo( IDoctor.class.getName() );

		try (SessionFactoryImplementor sessionFactory = domainModel.buildSessionFactory()) {
			final EntityPersister doctorPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( Doctor.class );
			final EntityRepresentationStrategy representationStrategy = doctorPersister.getRepresentationStrategy();
			assertThat( representationStrategy.getLoadJavaType().getJavaType() ).isEqualTo( Doctor.class );
			assertThat( representationStrategy.getProxyJavaType().getJavaType() ).isEqualTo( IDoctor.class );
		}
	}
}
