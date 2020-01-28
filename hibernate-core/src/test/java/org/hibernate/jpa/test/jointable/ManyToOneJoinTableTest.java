/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.jointable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SecondaryTable;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.hql.spi.QueryTranslator;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Christian Beikov
 */
public class ManyToOneJoinTableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Address.class,
				ResourceImpl.class,
				IssuerImpl.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
//		configuration.setProperty(AvailableSettings.OMIT_JOIN_OF_SUPERCLASS_TABLES, Boolean.FALSE.toString());
	}

	@Test
	public void testAvoidJoin() {
		final HQLQueryPlan plan = sessionFactory().getQueryPlanCache().getHQLQueryPlan(
				"SELECT e.id FROM Person e",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();
		// Ideally, we could detect that *ToOne join tables aren't used, but that requires tracking the uses of properties
		// Since *ToOne join tables are treated like secondary or subclass/superclass tables, the proper fix will allow many more optimizations
		assertFalse( generatedSql.contains( "join" ) );
	}

	@Test
	public void testRegression() {
		doInHibernate( this::sessionFactory, session -> {
			session.createNamedQuery( IssuerImpl.SELECT_RESOURCES_BY_ISSUER )
					.setParameter( "issuer", session.getReference( IssuerImpl.class, new Identifier( 1l, "ABC" ) ) )
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
		@JoinColumns({
				@JoinColumn(name = PARENT_ISSUER_COLUMN, table = TABLE_NAME, referencedColumnName = "issuer"),
				@JoinColumn(name = PARENT_IDENTIFIER_COLUMN, table = TABLE_NAME, referencedColumnName = "identifier")
		})
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
