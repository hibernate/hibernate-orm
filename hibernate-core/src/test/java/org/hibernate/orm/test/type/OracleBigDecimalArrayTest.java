/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(annotatedClasses = { OracleBigDecimalArrayTest.EntityWithBigDecimalArray.class })
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-17176")
public class OracleBigDecimalArrayTest {

	private final BigDecimal[] array = new BigDecimal[] { BigDecimal.ONE, BigDecimal.TEN };

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new OracleBigDecimalArrayTest.EntityWithBigDecimalArray( 1, array ) );
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityWithBigDecimalArray arrayHolder = session.find( EntityWithBigDecimalArray.class, 1 );
			assertThat( arrayHolder.bigDecimals ).isEqualTo( array );
		} );
	}

	@Test
	public void testNative(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Object[]> list = session.createNativeQuery( "select * from EntityWithBigDecimalArray" )
					.getResultList();
			assertThat( list.size() ).isEqualTo( 1 );
			assertThat( list.get( 0 )[0] ).isEqualTo( 1 );
			assertThat( list.get( 0 )[1] ).isEqualTo( array );
		} );
	}

	@Entity(name = "EntityWithBigDecimalArray")
	@Table(name = "EntityWithBigDecimalArray")
	public static class EntityWithBigDecimalArray {
		@Id
		private Integer id;

		@Column(columnDefinition = "MDSYS.SDO_ORDINATE_ARRAY")
		private BigDecimal[] bigDecimals;

		public EntityWithBigDecimalArray() {
		}

		public EntityWithBigDecimalArray(Integer id, BigDecimal[] bigDecimals) {
			this.id = id;
			this.bigDecimals = bigDecimals;
		}
	}

}
