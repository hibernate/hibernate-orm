package org.hibernate.orm.test.metamodel.mapping.quote;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@DomainModel(
		annotatedClasses = {
				ColumnQuoteTest.Product.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@ServiceRegistry.Setting( name = AvailableSettings.HBM2DDL_DATABASE_ACTION, value = "create-drop" ),
				@ServiceRegistry.Setting( name = AvailableSettings.JPA_SHARED_CACHE_MODE, value = "NONE" ),
				@ServiceRegistry.Setting( name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "false" )
		}
)
@SuppressWarnings( "unused" )
public class ColumnQuoteTest {

	private Long testProductId = 1L;

	private String testProductWhere = "USA";
	private String changedProductWhere = "China";

	private boolean testProductExists = true;
	private boolean changedProductExists = false;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = new Product( testProductId, testProductWhere, testProductExists );
			session.save( product );
		} );
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Product" ).executeUpdate();
		} );
	}

	@Test
	public void testSessionGet(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = session.get( Product.class, testProductId );
			assertThat( product.where, is( testProductWhere ) );
			assertThat( product.exists, is( testProductExists ) );
		} );
	}

	@Test
	public void testSelectHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = session.createQuery( "select p from Product p where p.id = :id", Product.class )
					.setParameter( "id", testProductId )
					.uniqueResult();
			assertThat( product.where, is( testProductWhere ) );
			assertThat( product.exists, is( testProductExists) );
		} );
	}

	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaQuery<Product> criteriaQuery = session.getCriteriaBuilder().createQuery( Product.class );
			criteriaQuery.select( criteriaQuery.from( Product.class ) );
			Product product = session.createQuery( criteriaQuery ).uniqueResult();
			assertThat( product.where, is( testProductWhere ) );
			assertThat( product.exists, is( testProductExists) );
		} );
	}

	@Test
	public void testSessionUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = session.get( Product.class, testProductId );
			product.where = changedProductWhere;
			product.exists = changedProductExists;
			session.flush();
		} );
		scope.inTransaction( session -> {
			Product product = session.get( Product.class, testProductId );
			assertThat( product.where, is( changedProductWhere ) );
			assertThat( product.exists, is( changedProductExists) );
		} );
	}

	@Test
	public void testUpdateHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "update Product p set p.where = :where, p.exists = :exists where p.id = :id" )
					.setParameter( "where", changedProductWhere )
					.setParameter( "exists", changedProductExists )
					.setParameter( "id", testProductId )
					.executeUpdate();
		} );

		scope.inTransaction( session -> {
			Product product = session.get( Product.class, testProductId );
			assertThat( product.where, is( changedProductWhere ) );
			assertThat( product.exists, is( changedProductExists) );
		} );
	}

	@Test
	@FailureExpected( jiraKey = "none", reason = "SqmUpdateStatement#set(String, Object value) not implemented yet" )
	public void testCriteriaUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaUpdate<Product> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Product.class );
			criteriaUpdate.from( Product.class );
			criteriaUpdate.set( "where", changedProductWhere ).set( "exists", changedProductExists );
			criteriaUpdate.where( criteriaBuilder.equal( criteriaUpdate.getRoot().get( "id" ), testProductId ) );
			session.createQuery( criteriaUpdate ).executeUpdate();
		} );

		scope.inTransaction( session -> {
			Product product = session.get( Product.class, testProductId );
			assertThat( product.where, is( changedProductWhere ) );
			assertThat( product.exists, is( changedProductExists) );
		} );
	}

	@Entity(name = "Product")
	@Table(name = "Product")
	public static class Product {
		@Id
		private Long id;

		/* The following two fields are chosen to coincide with SQL keywords */
		/* Without quoting, the testing case will fail (AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS has been disabled) */

		@Column(name = "`where`") // Hibernate quoting
		private String where;

		@Column(name = "\"exists\"") // JPA quoting
		private boolean exists;

		public Product() {
		}

		public Product(Long id, String where, boolean exists) {
			this.id = id;
			this.where = where;
			this.exists = exists;
		}

	}
}
