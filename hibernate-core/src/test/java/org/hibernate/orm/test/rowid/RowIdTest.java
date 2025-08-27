/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.rowid;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.RowId;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = RowIdTest.Product.class )
@SessionFactory(useCollectingStatementInspector = true)
public class RowIdTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = new Product();
			product.setId( "1L" );
			product.setName( "Mobile phone" );
			product.setNumber( "123-456-7890" );
			session.persist( product );
		} );
	}

	@Test
	void testRowId(SessionFactoryScope scope) {
		final String updatedName = "Smart phone";
		scope.inTransaction( session -> {
			String rowId = scope.getSessionFactory().getJdbcServices().getDialect().rowId("");

			SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
			statementInspector.clear();

			Product product = session.find( Product.class, "1L" );

			List<String> sqls = statementInspector.getSqlQueries();

			assertThat( sqls, hasSize( 1 ) );
			assertThat( rowId == null
					|| sqls.get(0).matches( "(?i).*\\bselect\\b.+\\." + rowId + ".*\\bfrom\\s+product\\b.*" ),
					is( true ) );

			assertThat( product.getName(), not( is( updatedName ) ) );

			product.setName( updatedName );

			statementInspector.clear();
			session.flush();

			sqls = statementInspector.getSqlQueries();

			assertThat( sqls, hasSize( 1 ) );
			assertThat(  rowId == null
					|| sqls.get( 0 ).matches( "(?i).*\\bupdate\\s+product\\b.+?\\bwhere\\s+" + rowId + "\\s*=.*" ),
					is( true ) );
		} );

		scope.inTransaction( session -> {
			Product product = session.createQuery( "from Product", Product.class ).uniqueResult();
			assertThat( product.getName(), is( updatedName ) );
		} );
	}

	@Entity(name = "Product")
	@Table(name = "product")
	@RowId
	public static class Product {

		@Id
		private String id;

		@Column(name = "`name`")
		private String name;

		@Column(name = "`number`")
		private String number;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

	}
}
