/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.CardPayment;
import org.hibernate.testing.orm.domain.retail.Payment;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see org.hibernate.persister.entity.mutation.EntityTableMapping
 *
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.RETAIL,
		annotatedClasses = {
				EntityTableMappingsTests.UnionRoot.class,
				EntityTableMappingsTests.UnionSub1.class,
				EntityTableMappingsTests.UnionSub2.class
		}
)
@SessionFactory(exportSchema = false)
public class EntityTableMappingsTests {
	@Test
	public void testSingleTableHierarchyTableDetails(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		verifyTableMappings(
				mappingMetamodel.getEntityDescriptor( Vendor.class ),
				"Vendor"
		);
	}

	@Test
	public void testJoinedHierarchyTableDetails(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();

		// root
		verifyTableMappings(
				mappingMetamodel.getEntityDescriptor( Payment.class ),
				"payments"
		);

		// sub
		verifyTableMappings(
				mappingMetamodel.getEntityDescriptor( CardPayment.class ),
				"payments",
				"CardPayment"
		);
	}

	@Test
	public void testUnionHierarchyTableDetails(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();

		// root
		final EntityPersister rootDescriptor = mappingMetamodel.getEntityDescriptor( UnionRoot.class );
		verifyTableMappings(
				rootDescriptor,
				"UnionRoot",
				"UnionRoot"
		);

		// sub1
		final EntityPersister sub1Descriptor = mappingMetamodel.getEntityDescriptor( UnionSub1.class );
		verifyTableMappings(
				sub1Descriptor,
				"unions_subs1",
				"unions_subs1"
		);

		// sub2
		final EntityPersister sub2Descriptor = mappingMetamodel.getEntityDescriptor( UnionSub2.class );
		verifyTableMappings(
				sub2Descriptor,
				"unions_subs2",
				"unions_subs2"
		);
	}

	private void verifyTableMappings(EntityMappingType entityDescriptor, String tableName) {
		verifyTableMappings( entityDescriptor, tableName, tableName );
	}

	private void verifyTableMappings(
			EntityMappingType entityDescriptor,
			String identifierTableName,
			String mappedTableName) {
		final TableDetails idTable = entityDescriptor.getIdentifierTableDetails();
		assertThat( idTable.getTableName() ).isEqualTo( identifierTableName );
		assertThat( idTable.isIdentifierTable() ).isTrue();
		assertThat( idTable.getKeyDetails().getColumnCount() ).isEqualTo( 1 );
		assertThat( idTable.getKeyDetails().getKeyColumn( 0 ).getColumnName() ).isEqualTo( "id" );

		final TableDetails mappedTable = entityDescriptor.getMappedTableDetails();
		assertThat( mappedTable.getTableName() ).isEqualTo( mappedTableName );
		assertThat( idTable.getKeyDetails().getColumnCount() ).isEqualTo( 1 );
		assertThat( idTable.getKeyDetails().getKeyColumn( 0 ).getColumnName() ).isEqualTo( "id" );
		if ( mappedTableName.equals( identifierTableName ) ) {
			assertThat( mappedTable.isIdentifierTable() ).isTrue();
		}
	}

	@Entity( name = "UnionRoot" )
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class UnionRoot {
		@Id
		private Integer id;
		@Basic
		private String name;

		private UnionRoot() {
			// for use by Hibernate
		}

		public UnionRoot(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "UnionSub1" )
	@Table( name = "unions_subs1" )
	public static class UnionSub1 extends UnionRoot {
		private UnionSub1() {
			// for use by Hibernate
		}

		public UnionSub1(Integer id, String name) {
			super( id, name );
		}
	}

	@Entity( name = "UnionSub2" )
	@Table( name = "unions_subs2" )
	public static class UnionSub2 extends UnionRoot {
		private UnionSub2() {
			// for use by Hibernate
		}

		public UnionSub2(Integer id, String name) {
			super( id, name );
		}
	}
}
