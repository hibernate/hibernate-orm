/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.associations;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.internal.ShortNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.AnyType;
import org.hibernate.type.MetaType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for singular Hibernate `@Any` association binding.
///
/// @author Steve Ebersole
@SuppressWarnings("removal")
public class AnyAssociationTests {
	@Test
	@ServiceRegistry
	void testExplicitAnyAssociation(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( Holder.class.getName() );
					final Property property = entityBinding.getProperty( "target" );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( value.isLazy() ).isFalse();
					assertThat( property.isOptional() ).isFalse();

					final BasicValue discriminator = value.getDiscriminatorDescriptor();
					assertThat( ( (Column) discriminator.getColumn() ).getName() ).isEqualTo( "target_type" );
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final BasicValue key = value.getKeyDescriptor();
					assertThat( ( (Column) key.getColumn() ).getName() ).isEqualTo( "target_id" );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					assertThat( value.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 1 ), TargetOne.class.getName() )
							.containsEntry( DiscriminatorValue.of( 2 ), TargetTwo.class.getName() );
				},
				scope.getRegistry(),
				Holder.class,
				TargetOne.class,
				TargetTwo.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyInfersKeyJavaClass(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( MissingKeyTypeHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					assertThat( value.getKeyDescriptor().resolve().getDomainJavaType().getJavaType() )
							.isEqualTo( Integer.class );
				},
				scope.getRegistry(),
				MissingKeyTypeHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyDiscriminatorCharType(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CharDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					final BasicValue discriminator = value.getDiscriminatorDescriptor();
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Character.class );
					assertThat( value.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 'A' ), TargetOne.class.getName() );
				},
				scope.getRegistry(),
				CharDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyImplicitDiscriminatorValues(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					assertThat( value.getMetaValues() ).isEmpty();
					assertThat( value.getDiscriminatorDescriptor().resolve().getDomainJavaType().getJavaType() )
							.isEqualTo( String.class );
					assertThat( ( (MetaType) ( (AnyType) value.getType() ).getDiscriminatorType() )
							.getImplicitValueStrategy() )
							.isSameAs( ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY );
				},
				scope.getRegistry(),
				ImplicitDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyDefaultsImplicitDiscriminatorValues(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( DefaultImplicitDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					assertThat( value.getMetaValues() ).isEmpty();
					assertThat( value.getDiscriminatorDescriptor().resolve().getDomainJavaType().getJavaType() )
							.isEqualTo( String.class );
					assertThat( ( (MetaType) ( (AnyType) value.getType() ).getDiscriminatorType() )
							.getImplicitValueStrategy() )
							.isSameAs( FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY );
				},
				scope.getRegistry(),
				DefaultImplicitDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyDiscriminatorJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue discriminator = value.getDiscriminatorDescriptor();

					assertThat( discriminator.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.CHAR );
				},
				scope.getRegistry(),
				JdbcTypeCodeDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyDiscriminatorFormula(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( FormulaDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					assertThat( value.getDiscriminatorDescriptor().getColumn() )
							.isInstanceOf( org.hibernate.mapping.Formula.class );
					assertThat( ( (org.hibernate.mapping.Formula) value.getDiscriminatorDescriptor().getColumn() )
							.getFormula() )
							.isEqualTo( "'one'" );
					assertThat( ( (Column) value.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_id" );
				},
				scope.getRegistry(),
				FormulaDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyIsNotOptionalWhenDiscriminatorColumnIsNotNullable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( NonNullableDiscriminatorHolder.class.getName() );
					final Property property = entityBinding.getProperty( "target" );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( property.isOptional() ).isFalse();
					assertThat( ( (Column) value.getDiscriminatorDescriptor().getColumn() ).isNullable() )
							.isFalse();
				},
				scope.getRegistry(),
				NonNullableDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyIsNotOptionalWhenKeyColumnIsNotNullable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( NonNullableKeyHolder.class.getName() );
					final Property property = entityBinding.getProperty( "target" );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( property.isOptional() ).isFalse();
					assertThat( ( (Column) value.getKeyDescriptor().getColumn() ).isNullable() )
							.isFalse();
				},
				scope.getRegistry(),
				NonNullableKeyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyKeyJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeKeyHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue key = value.getKeyDescriptor();

					assertThat( key.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.BIGINT );
				},
				scope.getRegistry(),
				JdbcTypeCodeKeyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyKeyJavaType(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JavaTypeKeyHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue key = value.getKeyDescriptor();

					assertThat( key.resolve().getDomainJavaType() )
							.isInstanceOf( AnyIntegerJavaType.class );
				},
				scope.getRegistry(),
				JavaTypeKeyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyCascade(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CascadeHolder.class.getName() );
					final Property property = entityBinding.getProperty( "target" );

					assertThat( property.getCascade() )
							.contains( "persist" )
							.contains( "merge" );
				},
				scope.getRegistry(),
				CascadeHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JoinTableHolder.class.getName() );
					final org.hibernate.mapping.Join join = entityBinding.getJoins().get( 0 );
					final Property property = join.getProperties().get( 0 );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( join.getTable().getName() ).isEqualTo( "any_holder_targets" );
					assertThat( join.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "holder_id" );
					assertThat( value.getTable() ).isSameAs( join.getTable() );
					assertThat( ( (Column) value.getDiscriminatorDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_type" );
					assertThat( ( (Column) value.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_id" );
				},
				scope.getRegistry(),
				JoinTableHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyImplicitJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinTableHolder.class.getName() );
					final org.hibernate.mapping.Join join = entityBinding.getJoins().get( 0 );
					final Property property = join.getProperties().get( 0 );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( join.getTable() ).isNotSameAs( entityBinding.getTable() );
					assertThat( join.getTable().getName() ).isEqualTo( "implicitjointableanyholder_target" );
					assertThat( join.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "holder_id" );
					assertThat( value.getTable() ).isSameAs( join.getTable() );
					assertThat( ( (Column) value.getDiscriminatorDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_type" );
					assertThat( ( (Column) value.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_id" );
				},
				scope.getRegistry(),
				ImplicitJoinTableHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyCompositeKeyColumns(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {},
				scope.getRegistry(),
				CompositeKeyHolder.class,
				CompositeKeyTarget.class
		) )
				.hasMessageContaining( "maps to 3 columns but 2 columns are required" );
	}

	@Test
	@ServiceRegistry
	void testAnyJoinTableCompositeKeyColumns(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {},
				scope.getRegistry(),
				JoinTableCompositeKeyHolder.class,
				CompositeKeyTarget.class
		) )
				.hasMessageContaining( "maps to 3 columns but 2 columns are required" );
	}

	@Test
	@ServiceRegistry
	void testManyToAnyAssociation(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ManyHolder.class.getName() );
					final Property property = entityBinding.getProperty( "targets" );
					final Collection collection = (Collection) property.getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "many_holder_targets" );
					assertThat( collection.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "holder_id" );

					final BasicValue discriminator = element.getDiscriminatorDescriptor();
					assertThat( ( (Column) discriminator.getColumn() ).getName() ).isEqualTo( "target_type" );
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final BasicValue key = element.getKeyDescriptor();
					assertThat( ( (Column) key.getColumn() ).getName() ).isEqualTo( "target_id" );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					assertThat( element.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 1 ), TargetOne.class.getName() )
							.containsEntry( DiscriminatorValue.of( 2 ), TargetTwo.class.getName() );
				},
				scope.getRegistry(),
				ManyHolder.class,
				TargetOne.class,
				TargetTwo.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyKeyJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeKeyManyHolder.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "targets" ).getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();
					final BasicValue key = element.getKeyDescriptor();

					assertThat( key.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.BIGINT );
				},
				scope.getRegistry(),
				JdbcTypeCodeKeyManyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyCompositeKeyColumns(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {},
				scope.getRegistry(),
				CompositeKeyManyHolder.class,
				CompositeKeyTarget.class
		) )
				.hasMessageContaining( "wrong number of columns" );
	}

	@Test
	@ServiceRegistry
	void testManyToAnyCascade(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CascadeManyHolder.class.getName() );
					final Property property = entityBinding.getProperty( "targets" );
					final Collection collection = (Collection) property.getValue();

					assertThat( property.getCascade() )
							.contains( "refresh" )
							.contains( "delete" );
					assertThat( collection.hasOrphanDelete() ).isFalse();
				},
				scope.getRegistry(),
				CascadeManyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyImplicitJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinTableManyHolder.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "targets" ).getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();

					assertThat( collection.getCollectionTable() ).isNotSameAs( entityBinding.getTable() );
					assertThat( collection.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "id" );
					assertThat( ( (Column) element.getDiscriminatorDescriptor().getColumn() ).getName() )
							.isEqualTo( "targets_type" );
					assertThat( ( (Column) element.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "targets_id" );
				},
				scope.getRegistry(),
				ImplicitJoinTableManyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapValuedManyToAnyAssociation(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( MapManyHolder.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding
							.getProperty( "targets" )
							.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "map_many_holder_targets" );
					assertThat( collection.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "holder_id" );
					assertThat( index.getColumns() ).extracting( Column::getName )
							.containsExactly( "target_key" );

					assertThat( ( (Column) element.getDiscriminatorDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_type" );
					assertThat( ( (Column) element.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "target_id" );
				},
				scope.getRegistry(),
				MapManyHolder.class,
				TargetOne.class,
				TargetTwo.class
		);
	}

	@Entity(name = "AnyHolder")
	public static class Holder {
		@Id
		private Integer id;

		@Any(optional = false)
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue(discriminator = "1", entity = TargetOne.class)
		@AnyDiscriminatorValue(discriminator = "2", entity = TargetTwo.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "MissingAnyKeyTypeHolder")
	public static class MissingKeyTypeHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		private Object target;
	}

	@Entity(name = "CharAnyDiscriminatorHolder")
	public static class CharDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminator(DiscriminatorType.CHAR)
		@AnyDiscriminatorValue(discriminator = "A", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "ImplicitAnyDiscriminatorHolder")
	public static class ImplicitDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorImplicitValues(AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME)
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "DefaultImplicitAnyDiscriminatorHolder")
	public static class DefaultImplicitDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JdbcTypeCodeAnyDiscriminatorHolder")
	public static class JdbcTypeCodeDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminator(DiscriminatorType.STRING)
		@AnyDiscriminatorValue(discriminator = "A", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@JdbcTypeCode(SqlTypes.CHAR)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "FormulaAnyDiscriminatorHolder")
	public static class FormulaDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@Formula("'one'")
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "NonNullableAnyDiscriminatorHolder")
	public static class NonNullableDiscriminatorHolder {
		@Id
		private Integer id;

		@Any(optional = true)
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type", nullable = false)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "NonNullableAnyKeyHolder")
	public static class NonNullableKeyHolder {
		@Id
		private Integer id;

		@Any(optional = true)
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		@JoinColumn(name = "target_id", nullable = false)
		private Object target;
	}

	@Entity(name = "JdbcTypeCodeAnyKeyHolder")
	public static class JdbcTypeCodeKeyHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJdbcTypeCode(SqlTypes.BIGINT)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JavaTypeAnyKeyHolder")
	public static class JavaTypeKeyHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJavaType(AnyIntegerJavaType.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "CascadeAnyHolder")
	public static class CascadeHolder {
		@Id
		private Integer id;

		@Any(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JoinTableAnyHolder")
	public static class JoinTableHolder {
		@Id
		private Integer id;

		@Any
		@JoinTable(
				name = "any_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@jakarta.persistence.Column(name = "target_type")
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		private Object target;
	}

	@Entity(name = "ImplicitJoinTableAnyHolder")
	public static class ImplicitJoinTableHolder {
		@Id
		private Integer id;

		@Any
		@JoinTable(
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@jakarta.persistence.Column(name = "target_type")
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		private Object target;
	}

	@Entity(name = "CompositeKeyAnyHolder")
	public static class CompositeKeyHolder {
		@Id
		private Integer id;

		@Any
		@JoinColumns({
				@JoinColumn(name = "target_id1"),
				@JoinColumn(name = "target_id2")
		})
		@jakarta.persistence.Column(name = "target_type")
		@AnyDiscriminatorValue(discriminator = "composite", entity = CompositeKeyTarget.class)
		@AnyKeyJavaClass(CompositeAnyKey.class)
		private Object target;
	}

	@Entity(name = "JoinTableCompositeKeyAnyHolder")
	public static class JoinTableCompositeKeyHolder {
		@Id
		private Integer id;

		@Any
		@JoinTable(
				name = "any_holder_composite_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = {
						@JoinColumn(name = "target_id1"),
						@JoinColumn(name = "target_id2")
				}
		)
		@jakarta.persistence.Column(name = "target_type")
		@AnyDiscriminatorValue(discriminator = "composite", entity = CompositeKeyTarget.class)
		@AnyKeyJavaClass(CompositeAnyKey.class)
		private Object target;
	}

	@Entity(name = "ManyAnyHolder")
	public static class ManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue(discriminator = "1", entity = TargetOne.class)
		@AnyDiscriminatorValue(discriminator = "2", entity = TargetTwo.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		private List<Object> targets;
	}

	@Entity(name = "CascadeManyAnyHolder")
	public static class CascadeManyHolder {
		@Id
		private Integer id;

		@ManyToAny(cascade = { CascadeType.REFRESH, CascadeType.REMOVE })
		@JoinTable(
				name = "cascade_many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		private List<Object> targets;
	}

	@Entity(name = "JdbcTypeCodeAnyKeyManyHolder")
	public static class JdbcTypeCodeKeyManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "jdbc_type_key_many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJdbcTypeCode(SqlTypes.BIGINT)
		private List<Object> targets;
	}

	@Entity(name = "CompositeKeyManyAnyHolder")
	public static class CompositeKeyManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "composite_key_many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = {
						@JoinColumn(name = "target_id1"),
						@JoinColumn(name = "target_id2")
				}
		)
		@AnyDiscriminatorValue(discriminator = "composite", entity = CompositeKeyTarget.class)
		@AnyKeyJavaClass(CompositeAnyKey.class)
		private List<Object> targets;
	}

	@Entity(name = "ImplicitJoinTableManyAnyHolder")
	public static class ImplicitJoinTableManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		private List<Object> targets;
	}

	@Entity(name = "MapManyAnyHolder")
	public static class MapManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "map_many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@MapKeyColumn(name = "target_key")
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyDiscriminatorValue(discriminator = "two", entity = TargetTwo.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		private Map<String, Object> targets;
	}

	@Entity(name = "AnyTargetOne")
	public static class TargetOne {
		@Id
		private Integer id;
	}

	@Entity(name = "AnyTargetTwo")
	public static class TargetTwo {
		@Id
		private Integer id;
	}

	@Entity(name = "CompositeAnyTarget")
	public static class CompositeKeyTarget {
		@EmbeddedId
		private CompositeAnyKey id;
	}

	public static class CompositeAnyKey implements Serializable {
		private Integer id1;
		private Integer id2;
	}

	public static class AnyIntegerJavaType extends AbstractClassJavaType<Integer> {
		public AnyIntegerJavaType() {
			super( Integer.class );
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.INTEGER );
		}

		@Override
		public Integer fromString(CharSequence string) {
			return string == null ? null : Integer.valueOf( string.toString() );
		}

		@Override
		public <X> X unwrap(Integer value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( type.isAssignableFrom( Integer.class ) ) {
				return type.cast( value );
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> Integer wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof Integer integer ) {
				return integer;
			}
			throw unknownWrap( value.getClass() );
		}
	}
}
