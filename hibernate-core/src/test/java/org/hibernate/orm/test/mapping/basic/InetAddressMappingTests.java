/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.net.InetAddress;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = InetAddressMappingTests.EntityWithInetAddress.class)
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "Driver or DB omit trailing zero bytes of a varbinary, making this test fail intermittently")
@SkipForDialect( dialectClass = InformixDialect.class,
		reason = "Blobs are not allowed in this expression (with a column of type BYTE)")
public class InetAddressMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) throws Exception {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithInetAddress.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("address");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(InetAddress.class));
		final JdbcType intervalType = jdbcTypeRegistry.getDescriptor(SqlTypes.INET);
		final JdbcType realType;
		if (intervalType instanceof AdjustableJdbcType) {
			realType = ((AdjustableJdbcType) intervalType).resolveIndicatedType(
					mappingMetamodel.getTypeConfiguration().getCurrentBaseSqlTypeIndicators(),
					jdbcMapping.getJavaTypeDescriptor()
			);
		}
		else {
			realType = intervalType;
		}
		assertThat( jdbcMapping.getJdbcType(), is( realType));

		EntityWithInetAddress entity = new EntityWithInetAddress( 1, InetAddress.getLocalHost() );
		scope.inTransaction(
				(session) -> {
					session.persist( entity );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithInetAddress.class, 1)
		);

		scope.inTransaction(
				(session) -> {
					session.createQuery(
									"from EntityWithInetAddress e where e.address = :param",
									EntityWithInetAddress.class
							)
							.setParameter( "param", entity.address )
							.getResultList();
				}
		);
	}

	@Entity(name = "EntityWithInetAddress")
	@Table(name = "EntityWithInetAddress")
	public static class EntityWithInetAddress {
		@Id
		private Integer id;

		//tag::basic-inet-address-example[]
		private InetAddress address;
		//end::basic-inet-address-example[]

		public EntityWithInetAddress() {
		}

		public EntityWithInetAddress(Integer id, InetAddress address) {
			this.id = id;
			this.address = address;
		}
	}
}
