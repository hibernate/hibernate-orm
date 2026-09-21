/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies derived-identifier names compose logical dependencies and apply physical naming once.
///
/// @author Steve Ebersole
class DerivedColumnNamingTest {
	@Test
	void derivedOverridesDoNotRepeatPhysicalNaming() {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			final var strategy = new PrefixStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, new MappingSources().addManagedClass( ReferringEntity.class ).addManagedClass( Parent.class )
							.addManagedClass( ExplicitChild.class ).addManagedClass( CompositeParent.class )
							.addManagedClass( ImplicitChild.class ).addManagedClass( QuotedChild.class ),
					new ImplicitNamingStrategyJpaCompliantImpl() {
						@Override
						public LogicalName determineMapKeyJoinColumnName(
								org.hibernate.boot.model.naming.spi.MapKeyJoinColumnNamingInput input,
								org.hibernate.boot.model.naming.spi.ImplicitNamingContext context) {
							assertThat( input.reference().column().logicalName().getText() ).isEqualTo( "parent_fk" );
							assertThat( input.reference().column().physicalName().getText() ).isEqualTo( "p_parent_fk" );
							return super.determineMapKeyJoinColumnName( input, context );
						}
					}, strategy );
			assertThat( metadata.getEntityBinding( ExplicitChild.class.getName() ).getIdentifier().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_parent_fk" );
			assertThat( metadata.getEntityBinding( ImplicitChild.class.getName() ).getIdentifier().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_parent_code", "p_parent_region" );
			final var quoted = metadata.getEntityBinding( QuotedChild.class.getName() ).getIdentifier().getColumns().get( 0 );
			assertThat( quoted.getName() ).isEqualTo( "p_QuotedFk" );
			assertThat( quoted.isQuoted() ).isTrue();
			assertThat( metadata.getEntityBinding( ReferringEntity.class.getName() ).getProperty( "child" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_child_parent_fk" );
			assertThat( metadata.getCollectionBinding( ReferringEntity.class.getName() + ".children" ).getElement().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_children_parent_fk" );
			assertThat( strategy.inputs ).filteredOn( "parent_fk"::equals ).hasSize( 1 );
			assertThat( strategy.inputs ).filteredOn( "parent_code"::equals ).hasSize( 1 );
			assertThat( strategy.inputs ).noneMatch( name -> name.startsWith( "p_" ) || name.startsWith( "parent_p_" ) );
		}
	}

	static class PrefixStrategy extends PhysicalNamingStrategyStandardImpl {
		final List<String> inputs = new ArrayList<>();
		@Override
		public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name.getText() );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity(name = "DerivedNamingReferrer")
	static class ReferringEntity {
		@Id Long id;
		@ManyToOne ExplicitChild child;
		@jakarta.persistence.ManyToMany java.util.Set<ExplicitChild> children;
		@jakarta.persistence.ElementCollection java.util.Map<ExplicitChild, String> labels;
	}

	@Entity(name = "DerivedNamingParent")
	static class Parent { @Id Long id; }
	@Entity(name = "DerivedNamingExplicitChild")
	static class ExplicitChild {
		@Id Long id;
		@MapsId @ManyToOne @JoinColumn(name = "parent_fk") Parent parent;
	}
	@Entity(name = "DerivedNamingQuotedChild")
	static class QuotedChild {
		@Id @jakarta.persistence.Column(name = "`QuotedId`") Long id;
		@MapsId @ManyToOne @JoinColumn(name = "`QuotedFk`") Parent parent;
	}

	@Embeddable
	static class Key implements java.io.Serializable { String code; String region; }
	@Entity(name = "DerivedNamingCompositeParent")
	static class CompositeParent { @EmbeddedId Key id; }
	@Entity(name = "DerivedNamingImplicitChild")
	static class ImplicitChild {
		@EmbeddedId Key id;
		@MapsId @ManyToOne CompositeParent parent;
	}
}
