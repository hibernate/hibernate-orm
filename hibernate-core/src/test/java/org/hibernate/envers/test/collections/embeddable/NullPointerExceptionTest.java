/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import java.util.Map;

import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.embeddable.Item;
import org.hibernate.envers.test.support.domains.collections.embeddable.Product;
import org.hibernate.envers.test.support.domains.collections.embeddable.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.junit5.envers.RequiresAuditStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11215")
@RequiresAuditStrategy(ValidityAuditStrategy.class)
public class NullPointerExceptionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer productId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Product.class, Type.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Product product = new Product( 1, "Test" );
					product.getItems().add( new Item( "bread", null ) );
					entityManager.persist( product );
					this.productId = product.getId();
				},

				// Revision 2
				entityManager -> {
					Type type = new Type( 2, "T2" );
					entityManager.persist( type );
					Product product = entityManager.find( Product.class, productId );
					product.getItems().add( new Item( "bread2", type ) );
					entityManager.merge( product );
				},

				// Revision 3
				entityManager -> {
					Product product = entityManager.find( Product.class, productId );
					product.getItems().remove( 0 );
					entityManager.merge( product );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Product.class, productId ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().find( Product.class, productId, 1 ).getItems(), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().find( Product.class, productId, 2 ).getItems(), CollectionMatchers.hasSize( 2 ) );
		assertThat( getAuditReader().find( Product.class, productId, 3 ).getItems(), CollectionMatchers.hasSize( 1 ) );
	}

	@DynamicTest
	public void testRevision1() {
		final Product product = getAuditReader().find( Product.class, productId, 1 );
		assertThat( product.getItems(), contains( new Item( "bread", null ) ) );
	}

	@DynamicTest
	public void testRevision2() {
		final Product product = getAuditReader().find( Product.class, productId, 2 );
		assertThat(
				product.getItems(),
				contains(
						new Item( "bread", null ),
						new Item( "bread2", new Type( 2, "T2" ) )
				)
		);
	}

	@DynamicTest
	public void testRevision3() {
		final Product product = getAuditReader().find( Product.class, productId, 3 );
		assertThat( product.getItems(), contains( new Item( "bread2", new Type( 2, "T2" ) ) ) );
	}
}

