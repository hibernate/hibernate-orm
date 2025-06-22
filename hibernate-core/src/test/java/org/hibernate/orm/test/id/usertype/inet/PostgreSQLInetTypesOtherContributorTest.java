/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLInetTypesOtherContributorTest extends PostgreSQLInetTypesOtherTest {

	@Override
	protected void addConfigOptions(Map options) {
		options.put( EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR, new InetTypeMetadataBuilderContributor() );
	}

	@Test
	public void testTypeContribution() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Inet> inets = entityManager.createNativeQuery(
				"select e.ip " +
				"from Event e " +
				"where e.id = :id" )
			.setParameter( "id", 1L )
			.getResultList();

			assertEquals( 1, inets.size() );
			assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}

	public class InetTypeMetadataBuilderContributor
			implements MetadataBuilderContributor {

		@Override
		public void contribute(MetadataBuilder metadataBuilder) {
			final TypeConfiguration typeConfiguration =
					( (MetadataBuilderImplementor) metadataBuilder )
							.getBootstrapContext()
							.getTypeConfiguration();
			typeConfiguration.getJavaTypeRegistry().addDescriptor( InetJavaType.INSTANCE );
			typeConfiguration.getJdbcTypeRegistry().addDescriptor( InetJdbcType.INSTANCE );
			metadataBuilder.applyBasicType(
					InetType.INSTANCE, "inet"
			);
		}
	}
}
