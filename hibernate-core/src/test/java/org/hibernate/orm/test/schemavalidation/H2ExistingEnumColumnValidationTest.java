package org.hibernate.orm.test.schemavalidation;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static jakarta.persistence.GenerationType.IDENTITY;

@JiraKey("HHH-17675")
@RequiresDialect(H2Dialect.class)
public class H2ExistingEnumColumnValidationTest extends BaseCoreFunctionalTestCase {

	private StandardServiceRegistry ssr;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityE.class };
	}

	@Before
	public void setUp() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "DROP TABLE IF EXISTS en CASCADE" ).executeUpdate();
			session.createNativeQuery(
							"CREATE TABLE en (id INTEGER NOT NULL AUTO_INCREMENT, sign_position enum ('AFTER_NO_SPACE','AFTER_WITH_SPACE','BEFORE_NO_SPACE','BEFORE_WITH_SPACE'), PRIMARY KEY (id))" )
					.executeUpdate();
		} );
	}

	@After
	public void tearDown() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "DROP TABLE en CASCADE" ).executeUpdate();
		} );
	}

	@Test
	public void testEnumDataTypeSchemaValidator() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "validate" )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( EntityE.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}


	@Entity(name = "en")
	@Table(name = "en")
	public static class EntityE {
		@Id
		@GeneratedValue(strategy = IDENTITY)
		@Column(name = "id", nullable = false, updatable = false)
		private Integer id;

		@Enumerated(EnumType.STRING)
		@Column(name = "sign_position")
		private SignPosition signPosition;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SignPosition getSignPosition() {
			return signPosition;
		}

		public void setSignPosition(SignPosition signPosition) {
			this.signPosition = signPosition;
		}
	}

	public enum SignPosition {
		AFTER_NO_SPACE, AFTER_WITH_SPACE, BEFORE_NO_SPACE, BEFORE_WITH_SPACE
	}
}
