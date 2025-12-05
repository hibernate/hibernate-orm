/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@DomainModel(
		annotatedClasses = {
				MaterializedBlobEntity.class
		}
)
@SessionFactory
public class MaterializedBlobTest {

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "Blob in CockroachDB is same as a varbinary, to assertions will fail")
	public void testTypeSelection(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( MaterializedBlobEntity.class.getName() );
		final AttributeMapping theBytesAttr = entityDescriptor.findAttributeMapping( "theBytes" );
		assertThat( theBytesAttr ).isInstanceOf( BasicValuedModelPart.class );
		final JdbcMapping mapping = ((BasicValuedModelPart) theBytesAttr).getJdbcMapping();
		assertThat( mapping.getJavaTypeDescriptor() ).isInstanceOf( PrimitiveByteArrayJavaType.class );
		assertThat( mapping.getJdbcType() ).isInstanceOf( BlobJdbcType.class );
	}

	@Test
	public void testSaving(SessionFactoryScope scope) {
		byte[] testData = "test data".getBytes();

		MaterializedBlobEntity e = new MaterializedBlobEntity( "test", testData );
		scope.inTransaction(
				session ->
						session.persist( e )
		);

		scope.inTransaction(
				session -> {
					MaterializedBlobEntity entity = session.find( MaterializedBlobEntity.class, e.getId() );
					assertThat( entity.getTheBytes() ).isEqualTo( testData );
					session.remove( entity );
				}
		);
	}
}
