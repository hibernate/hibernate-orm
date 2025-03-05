/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.joinable;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SecondaryTable;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneJoinTableTest.ResourceImpl.class,
				ManyToOneJoinTableTest.IssuerImpl.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
@SessionFactory
public class ManyToOneJoinTableTest implements SessionFactoryProducer {
	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sqlStatementInterceptor = new SQLStatementInterceptor( sessionFactoryBuilder );
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}


	@Test
	public void testRegression(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNamedQuery( IssuerImpl.SELECT_RESOURCES_BY_ISSUER )
							.setParameter(
									"issuer",
									session.getReference( IssuerImpl.class, new Identifier( 1l, "ABC" ) )
							)
							.getResultList();
				} );
	}

	public interface Issuer extends Resource {
	}

	@Entity(name = IssuerImpl.ENTITY_NAME)
	@SecondaryTable(name = IssuerImpl.TABLE_NAME)
	@NamedQuery(name = IssuerImpl.SELECT_RESOURCES_BY_ISSUER, query = "SELECT resource.identifier FROM " + ResourceImpl.ENTITY_NAME + " resource WHERE resource.identifier.issuer IN (SELECT issuer.identifier.issuer FROM " + IssuerImpl.ENTITY_NAME + " issuer WHERE issuer.parentIssuer = :issuer OR issuer = :issuer)")
	public static class IssuerImpl extends ResourceImpl implements Issuer {

		private static final String SELECT_RESOURCES_BY_ISSUER = "SELECT_RESOURCES_BY_ISSUER";
		private static final String ENTITY_NAME = "Issuer";
		public static final String PARENT_ISSUER_COLUMN = "parent_issuer";
		public static final String PARENT_IDENTIFIER_COLUMN = "parent_identifier";
		public static final String TABLE_NAME = "issuer_impl";

		@ManyToOne(targetEntity = IssuerImpl.class)
		@JoinColumn(name = PARENT_ISSUER_COLUMN, table = TABLE_NAME, referencedColumnName = "issuer")
		@JoinColumn(name = PARENT_IDENTIFIER_COLUMN, table = TABLE_NAME, referencedColumnName = "identifier")
		private Issuer parentIssuer;

		public Identifier getIdentifier() {
			return identifier;
		}

		public void setIdentifier(Identifier identifier) {
			this.identifier = identifier;
		}

		public Issuer getParentIssuer() {
			return parentIssuer;
		}

		public void setParentIssuer(Issuer parentIssuer) {
			this.parentIssuer = parentIssuer;
		}
	}

	@Embeddable
	public static class Identifier implements Serializable {
		Long issuer;
		String identifier;

		public Long getIssuer() {
			return issuer;
		}

		public void setIssuer(Long issuer) {
			this.issuer = issuer;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public Identifier() {

		}

		public Identifier(Long issuer, String identifier) {
			this.issuer = issuer;
			this.identifier = identifier;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Identifier that = (Identifier) o;
			return Objects.equals( issuer, that.issuer ) &&
					Objects.equals( identifier, that.identifier );
		}

		@Override
		public int hashCode() {
			return Objects.hash( issuer, identifier );
		}
	}

	public interface Resource {

	}

	@Entity(name = ResourceImpl.ENTITY_NAME)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class ResourceImpl implements Resource {

		private static final String ENTITY_NAME = "TestResource";

		@EmbeddedId
		Identifier identifier;

		public Identifier getIdentifier() {
			return identifier;
		}

		public void setIdentifier(Identifier identifier) {
			this.identifier = identifier;
		}
	}
}
