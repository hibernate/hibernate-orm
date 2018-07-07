/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.BatchSize;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

@RequiresDialect(value = { PostgreSQL81Dialect.class })
@TestForIssue(jiraKey = "HHH-12647")
public class PostgresArrayBatchTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Model.class,
			ProductLine.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.ANY_ARRAY.name() );
	}

	@Before
	public void init() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		for (int ii = 0; ii < 100; ii++) {
			ProductLine pl = new ProductLine();
			pl.setId(UUID.randomUUID().toString());
			pl.setDescription("Product line " + ii);
			s.save(pl);

			Model model1 = new Model();
			model1.setId(UUID.randomUUID().toString());
			model1.setName("Model 1 Line " + ii);
			model1.setDescription("Description " + ii);
			model1.setProductLine(pl);
			s.save(model1);

			Model model2 = new Model();
			model2.setId(UUID.randomUUID().toString());
			model2.setName("Model 2 Line " + ii);
			model2.setDescription("Description " + ii);
			model2.setProductLine(pl);
			s.save(model2);
		}

		t.commit();
		s.close();
		sqlStatementInterceptor.getSqlQueries().clear();
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testBatchFetchPostgres1() {
		Session s = openSession();

		List<ProductLine> list = (List<ProductLine>)s.createQuery( "from ProductLine pl" ).list();
		assertEquals( list.size(), 100 );
		int modelsCount = 0;
		for (ProductLine productLine : list) {
			assertNotNull(productLine.getDescription());
			for (Model model : (Set<Model>)productLine.getModels()) {
				assertNotNull(model.getName());
				modelsCount++;
			}
		}
		assertEquals( 200, modelsCount);
		s.close();

		// Expecting 3 SELECTs, 1 on ProductLine, 2 identical on Model
		List<String> selects = sqlStatementInterceptor.getSqlQueries();
		assertEquals( 3, selects.size() );
		assertTrue ( selects.get(0).contains("product_line") );
		assertTrue ( selects.get(1).contains("model") );
		assertTrue ( selects.get(1).contains("= ANY(?)") );
		assertEquals ( selects.get(1), selects.get(2) );
	}

	@SuppressWarnings({ "unchecked" })
	@Test
	public void testBatchFetchPostgres2() {
		Session s = openSession();

		List<Model> list = (List<Model>)s.createQuery( "from Model m" ).list();
		assertEquals( list.size(), 200 );
		for (Model model : list) {
			assertNotNull(model.getName());
			assertNotNull(model.getProductLine().getDescription());
		}
		s.close();

		// Expecting 3 SELECTs, 1 on Model, 2 identical on ProductLine
		List<String> selects = sqlStatementInterceptor.getSqlQueries();
		assertEquals( 3, selects.size() );
		assertTrue ( selects.get(0).contains("model") );
		assertTrue ( selects.get(1).contains("product_line") );
		assertTrue ( selects.get(1).contains("= ANY(?)") );
		assertEquals ( selects.get(1), selects.get(2) );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@BatchSize(size = 64)
	@Entity(name = "Model")
	@Table(name = "model")
	public static class Model {
		@Id
		@Column(name = "model_id", length = 36)
		private String id;

		private String name;
		private String description;

		@BatchSize(size = 64)
		@JoinColumn(name = "product_id")
		@ManyToOne(targetEntity = ProductLine.class)
		private ProductLine productLine;

		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
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
		public ProductLine getProductLine() {
			return productLine;
		}
		public void setProductLine(ProductLine productLine) {
			this.productLine = productLine;
		}
	}


	@BatchSize(size = 64)
	@Entity(name = "ProductLine")
	@Table(name = "product_line")
	public static class ProductLine {
		@Id
		@Column(name = "product_id", length = 36)
		private String id;

		private String description;

		@OneToMany(targetEntity = Model.class, mappedBy = "productLine")
		@BatchSize(size = 64)
		private Set<Model> models;

		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Set<Model> getModels() {
			return models;
		}
	}
}
