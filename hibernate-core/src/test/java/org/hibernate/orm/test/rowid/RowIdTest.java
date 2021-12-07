/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.rowid;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.RowId;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
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
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
@RequiresDialect( value = OracleDialect.class, majorVersion = 9)
public class RowIdTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = new Product();
			product.setId( 1L );
			product.setName( "Mobile phone" );
			product.setNumber( "123-456-7890" );
			session.persist( product );
		} );
	}

	@Test
	void testRowId(SessionFactoryScope scope) {
		final String updatedName = "Smart phone";
		scope.inTransaction( session -> {
			SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
			statementInspector.clear();

			Product product = session.find( Product.class, 1L );

			List<String> sqls = statementInspector.getSqlQueries();

			assertThat( sqls, hasSize( 1 ) );
			assertThat( sqls.get(0).matches( "(?i).*\\bselect\\b.+\\.ROWID.*\\bfrom\\s+product\\b.*" ), is( true ) );

			assertThat( product.getName(), not( is( updatedName ) ) );

			product.setName( updatedName );

			statementInspector.clear();
			session.flush();

			sqls = statementInspector.getSqlQueries();

			assertThat( sqls, hasSize( 1 ) );
			assertThat( sqls.get( 0 ).matches( "(?i).*\\bupdate\\s+product\\b.+?\\bwhere\\s+ROWID\\s*=.*" ), is( true ) );
		} );

		scope.inTransaction( session -> {
			Product product = session.createQuery( "from Product", Product.class ).uniqueResult();
			assertThat( product.getName(), is( updatedName ) );
		} );
	}

	@Entity(name = "Product")
	@Table(name = "product")
	@RowId("ROWID")
	public static class Product {

		@Id
		private Long id;

		@Column(name = "`name`")
		private String name;

		@Column(name = "`number`")
		private String number;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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
