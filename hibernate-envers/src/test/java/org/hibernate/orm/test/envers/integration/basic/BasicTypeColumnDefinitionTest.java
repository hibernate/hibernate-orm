/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.engine.jdbc.Size.DEFAULT_LENGTH;
import static org.hibernate.engine.jdbc.Size.DEFAULT_PRECISION;
import static org.hibernate.engine.jdbc.Size.DEFAULT_SCALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test verifies that resolving a column mapping's {@code sql-type} for HBM XML is performed
 * correctly such that when a column supplies a {@code columnDefinition}, Envers properly builds
 * its schema based on the right type rather than directly using the column definition as-is.
 * <p>
 * The following illustrate some examples of expected transformations:
 *
 * <li>{@code @Column(columnDefinition = "varchar(10) not null")} => {@code sql-type = "varchar(255)"}</li>
 * <li>{@code @Column(length = 10, columnDefinition = "varchar(10) not null")} => {@code sql-type = "varchar(10)"}</li>
 * <li>{@code @Column(columnDefinition = "integer not null auto_increment")} => {@code sql-type = "integer"}</li>
 * <p>
 * It is important to point out that resolving the sql-types length/precision/scale is all based on the
 * values supplied as part of the {@link Column} annotation itself and not what is in the definition text.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10844")
@RequiresDialect(H2Dialect.class)
@EnversTest
@DomainModel(annotatedClasses = {BasicTypeColumnDefinitionTest.BasicTypeContainer.class})
@SessionFactory
public class BasicTypeColumnDefinitionTest {
	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicTypeContainer entity = new BasicTypeContainer();
			entity.setData( "test" );
			entity.setColumnWithDefinition( "1234567890" );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final BasicTypeContainer entity = session.createQuery( "from BasicTypeContainer", BasicTypeContainer.class )
					.getResultList().get( 0 );
			entity.setData( "test2" );
			session.merge( entity );
		} );
	}

	// By reverting changes for HHH-10844 to restore columnDefinition original behavior, this implies this test will
	// now fail because the expected sql-type will once again be identical to the base table mapping.
	@Test
	public void testMetadataBindings(DomainModelScope scope) {
		final var domainModel = scope.getDomainModel();

		assertThrows( AssertionFailedError.class, () -> {
			final Long expectedDefaultLength = DEFAULT_LENGTH;
			final Integer expectedDefaultPrecision = DEFAULT_PRECISION;
			final Integer expectedDefaultScale = DEFAULT_SCALE;

			final Table auditTable = domainModel.getEntityBinding( BasicTypeContainer.class.getName() + "_AUD" )
					.getTable();

			final org.hibernate.mapping.Column caseNumber = auditTable.getColumn( toIdentifier( "caseNumber" ) );
			assertEquals( "integer", caseNumber.getSqlType() );
			assertEquals( expectedDefaultLength, caseNumber.getLength() );
			assertEquals( expectedDefaultPrecision, caseNumber.getPrecision() );
			assertEquals( expectedDefaultScale, caseNumber.getScale() );

			final org.hibernate.mapping.Column colDef = auditTable.getColumn( toIdentifier( "columnWithDefinition" ) );
			assertEquals( "varchar(10)", colDef.getSqlType() );
			assertEquals( Long.valueOf( 10 ), colDef.getLength() );
			assertEquals( expectedDefaultPrecision, colDef.getPrecision() );
			assertEquals( expectedDefaultScale, colDef.getScale() );
		} );
	}

	@Test
	public void testRevisionHistory(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( 2, auditReader.getRevisions( BasicTypeContainer.class, 1 ).size() );

			final BasicTypeContainer rev1 = auditReader.find( BasicTypeContainer.class, 1, 1 );
			assertEquals( "test", rev1.getData() );
			assertEquals( "1234567890", rev1.getColumnWithDefinition() );
			assertEquals( Integer.valueOf( 1 ), rev1.getCaseNumber() );

			final BasicTypeContainer rev2 = auditReader.find( BasicTypeContainer.class, 1, 2 );
			assertEquals( "test2", rev2.getData() );
			assertEquals( "1234567890", rev2.getColumnWithDefinition() );
			assertEquals( Integer.valueOf( 1 ), rev2.getCaseNumber() );
		} );
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
