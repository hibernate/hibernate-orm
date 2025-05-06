/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.Audited;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.ComparisonFailure;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.engine.jdbc.Size.DEFAULT_LENGTH;
import static org.hibernate.engine.jdbc.Size.DEFAULT_PRECISION;
import static org.hibernate.engine.jdbc.Size.DEFAULT_SCALE;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * This test verifies that resolving a column mapping's {@code sql-type} for HBM XML is performed
 * correctly such that when a column supplies a {@code columnDefinition}, Envers properly builds
 * its schema based on the right type rather than directly using the column definition as-is.
 *
 * The following illustrate some examples of expected transformations:
 *
 * <li>{@code @Column(columnDefinition = "varchar(10) not null")} => {@code sql-type = "varchar(255)"}</li>
 * <li>{@code @Column(length = 10, columnDefinition = "varchar(10) not null")} => {@code sql-type = "varchar(10)"}</li>
 * <li>{@code @Column(columnDefinition = "integer not null auto_increment")} => {@code sql-type = "integer"}</li>
 *
 * It is important to point out that resolving the sql-types length/precision/scale is all based on the
 * values supplied as part of the {@link Column} annotation itself and not what is in the definition text.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10844")
@RequiresDialect(value = H2Dialect.class)
public class BasicTypeColumnDefinitionTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicTypeContainer.class };
	}

	// By reverting changes for HHH-10844 to restore columnDefinition original behavior, this implies this test will
	// now fail because the expected sql-type will once again be identical to the base table mapping.
	@Test(expected = ComparisonFailure.class)
	@Priority(10)
	public void testMetadataBindings() {
		final Long expectedDefaultLength = new Long( DEFAULT_LENGTH );
		final Integer expectedDefaultPrecision = Integer.valueOf( DEFAULT_PRECISION );
		final Integer expectedDefaultScale = Integer.valueOf( DEFAULT_SCALE );

		final Table auditTable = metadata().getEntityBinding( BasicTypeContainer.class.getName() + "_AUD" ).getTable();

		final org.hibernate.mapping.Column caseNumber = auditTable.getColumn( toIdentifier( "caseNumber" ) );
		assertEquals( "integer", caseNumber.getSqlType() );
		assertEquals( expectedDefaultLength, caseNumber.getLength() );
		assertEquals( expectedDefaultPrecision, caseNumber.getPrecision() );
		assertEquals( expectedDefaultScale, caseNumber.getScale() );

		final org.hibernate.mapping.Column colDef = auditTable.getColumn( toIdentifier( "columnWithDefinition" ) );
		assertEquals( "varchar(10)", colDef.getSqlType() );
		assertEquals( new Long( 10 ), colDef.getLength() );
		assertEquals( expectedDefaultPrecision, colDef.getPrecision() );
		assertEquals( expectedDefaultScale, colDef.getScale() );
	}

	@Test
	@Priority(10)
	public void initData() {
		final BasicTypeContainer detachedEntity = doInJPA( this::entityManagerFactory, entityManager -> {
			final BasicTypeContainer entity = new BasicTypeContainer();
			entity.setData( "test" );
			entity.setColumnWithDefinition( "1234567890" );
			entityManager.persist( entity );
			return entity;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final BasicTypeContainer entity = entityManager.find( BasicTypeContainer.class, detachedEntity.getId() );
			entity.setData( "test2" );
			entityManager.merge( entity );
		} );
	}

	@Test
	public void testRevisionHistory() {
		assertEquals( 2, getAuditReader().getRevisions( BasicTypeContainer.class, 1 ).size() );

		final BasicTypeContainer rev1 = getAuditReader().find( BasicTypeContainer.class, 1, 1 );
		assertEquals( "test", rev1.getData() );
		assertEquals( "1234567890", rev1.getColumnWithDefinition() );
		assertEquals( Integer.valueOf( 1 ), rev1.getCaseNumber() );

		final BasicTypeContainer rev2 = getAuditReader().find( BasicTypeContainer.class, 1, 2 );
		assertEquals( "test2", rev2.getData() );
		assertEquals( "1234567890", rev2.getColumnWithDefinition() );
		assertEquals( Integer.valueOf( 1 ), rev2.getCaseNumber() );
	}

	@Entity(name = "BasicTypeContainer")
	@Audited
	public static class BasicTypeContainer {
		@Id
		@GeneratedValue
		private Integer id;

		@Generated
		@Column(name = "caseNumber", columnDefinition = "integer not null auto_increment")
		private Integer caseNumber;

		@Column(name = "columnWithDefinition", length = 10, nullable = false, columnDefinition = "varchar(10) not null")
		private String columnWithDefinition;

		private String data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getCaseNumber() {
			return caseNumber;
		}

		public void setCaseNumber(Integer caseNumber) {
			this.caseNumber = caseNumber;
		}

		public String getColumnWithDefinition() {
			return columnWithDefinition;
		}

		public void setColumnWithDefinition(String columnWithDefinition) {
			this.columnWithDefinition = columnWithDefinition;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
