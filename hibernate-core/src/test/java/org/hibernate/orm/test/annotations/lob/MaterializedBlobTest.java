/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class MaterializedBlobTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MaterializedBlobEntity.class };
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "Blob in CockroachDB is same as a varbinary, to assertions will fail")
	public void testTypeSelection() {
		final EntityPersister entityDescriptor = sessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( MaterializedBlobEntity.class.getName() );
		final AttributeMapping theBytesAttr = entityDescriptor.findAttributeMapping( "theBytes" );
		assertThat( theBytesAttr ).isInstanceOf( BasicValuedModelPart.class );
		final JdbcMapping mapping = ( (BasicValuedModelPart) theBytesAttr ).getJdbcMapping();
		assertTrue( mapping.getJavaTypeDescriptor() instanceof PrimitiveByteArrayJavaType );
		assertTrue( mapping.getJdbcType() instanceof BlobJdbcType );
	}

	@Test
	public void testSaving() {
		byte[] testData = "test data".getBytes();

		Session session = openSession();
		session.beginTransaction();
		MaterializedBlobEntity entity = new MaterializedBlobEntity( "test", testData );
		session.persist( entity );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		entity = session.get( MaterializedBlobEntity.class, entity.getId() );
		assertTrue( Arrays.equals( testData, entity.getTheBytes() ) );
		session.remove( entity );
		session.getTransaction().commit();
		session.close();
	}
}
