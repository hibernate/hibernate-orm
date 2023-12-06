/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.inheritance;

import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class UnionSubclassTests {
	@Test
	@ServiceRegistry
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleModel(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( UnionRoot.class.getName() );
					assertThat( rootBinding ).isSameAs( rootBinding.getRootClass() );
					assertThat( rootBinding.getTable() ).isSameAs( rootBinding.getRootTable() );
					assertThat( rootBinding.getTable() ).isInstanceOf( org.hibernate.mapping.Table.class );
					assertThat( rootBinding.getTable().isAbstract() ).isFalse();
					assertThat( rootBinding.getTable().getName() ).isEqualToIgnoringCase( "unionroot" );

					final PersistentClass subBinding = metadataCollector.getEntityBinding( UnionSub.class.getName() );
					assertThat( subBinding ).isNotSameAs( subBinding.getRootClass() );
					assertThat( subBinding.getRootClass() ).isSameAs( rootBinding );
					assertThat( subBinding.getTable() ).isNotSameAs( subBinding.getRootTable() );
					assertThat( rootBinding.getTable() ).isSameAs( subBinding.getRootTable() );
					assertThat( subBinding.getTable() ).isInstanceOf( org.hibernate.mapping.DenormalizedTable.class );
					assertThat( subBinding.getTable().isAbstract() ).isFalse();
					assertThat( subBinding.getTable().getName() ).isEqualToIgnoringCase( "UnionSubclassTests$UnionSub" );

					assertThat( rootBinding.getIdentifier() ).isNotNull();
					assertThat( rootBinding.getTable().getPrimaryKey() ).isNotNull();

					assertThat( subBinding.getIdentifier() ).isNotNull();
					assertThat( subBinding.getTable().getPrimaryKey() ).isNotNull();
				},
				scope.getRegistry(),
				UnionRoot.class,
				UnionSub.class
		);
	}

	/**
	 * @author Steve Ebersole
	 */
	@Entity
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name = "unionroot")
	public static class UnionRoot {
		@Id
		private Integer id;
		private String name;
	}

	/**
	 * @author Steve Ebersole
	 */
	@Entity
	public static class UnionSub extends UnionRoot {
		private String unionData;
	}
}
