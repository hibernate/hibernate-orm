/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.id;

import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
@SuppressWarnings("JUnitMalformedDeclaration")
public class IdBindingTests {
	@Test
	void testBasicId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( BasicIdEntity.class.getName() );

					final PrimaryKey primaryKey = rootBinding.getIdentityTable().getPrimaryKey();
					final KeyValue identifier = rootBinding.getIdentifier();

					assertThat( primaryKey.getColumns() ).hasSize( 1 );
					assertThat( identifier.getColumns() ).hasSize( 1 );
					assertThat( identifier.getColumns().get( 0 ) ).isSameAs( primaryKey.getColumns().get( 0 ) );
					assertThat( identifier.getColumns().get( 0 ).getName() ).isEqualToIgnoringCase( "id" );
				},
				scope.getRegistry(),
				BasicIdEntity.class
		);
	}

	@Test
	@FailureExpected(reason = "Embeddables not yet supported")
	void testAggregatedId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass rootBinding = (RootClass) metadataCollector.getEntityBinding( AggregatedIdEntity.class.getName() );

					final PrimaryKey primaryKey = rootBinding.getIdentityTable().getPrimaryKey();
					final KeyValue identifier = rootBinding.getIdentifier();

					assertThat( primaryKey.getColumns() ).hasSize( 2 );
					assertThat( identifier.getColumns() ).hasSize( 2 );
					assertThat( identifier.getColumns() ).containsAll( primaryKey.getColumns() );
				},
				scope.getRegistry(),
				AggregatedIdEntity.class
		);
	}
}
