/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Andrea Boriero
 */
public class ColumnInsertableFalseTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Price.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testSavingAndUpdating() {
		sessionFactoryScope().inTransaction(
				session -> {
					Price price = new Price( 1, "first", 12 );
					session.save( price );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					assertThat( session.get( Price.class, 1 ).getInitalPrice(), equalTo( 12 ) );
					assertThat( session.get( Price.class, 1 ).getDescription(), nullValue() );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					Price price = session.get( Price.class, 1 );
					session.update( price );
					price.setInitalPrice( 20 );
					price.setDescription( "first item" );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					assertThat( session.get( Price.class, 1 ).getInitalPrice(), equalTo( 20 ) );
					assertThat( session.get( Price.class, 1 ).getDescription(), nullValue() );
				}
		);
	}

	@Entity
	public static class Price {
		@Id
		private Integer id;

		@Column(insertable = false, updatable = false)
		private String description;

		private Integer initalPrice;

		public Price() {
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public Price(Integer id, String description, Integer initalPrice) {
			this.id = id;
			this.description = description;
			this.initalPrice = initalPrice;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getInitalPrice() {
			return initalPrice;
		}

		public void setInitalPrice(Integer initalPrice) {
			this.initalPrice = initalPrice;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
