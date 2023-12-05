/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

/**
 * @author Steve Ebersole
 */
public class SubclassEntityBinding extends EntityBinding {
	private final Subclass subclass;

	public SubclassEntityBinding(
			EntityTypeMetadata typeMetadata,
			IdentifiableTypeBinding superTypeBinding,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( typeMetadata, superTypeBinding, hierarchyRelation, bindingOptions, bindingState, bindingContext );

		this.subclass = createSubclass();
		applyNaming( typeMetadata, subclass, bindingState );
		bindingState.registerTypeBinding( getTypeMetadata(), this );

		if ( subclass instanceof TableOwner ) {
			final var primaryTable = TableHelper.bindPrimaryTable(
					typeMetadata,
					EntityHierarchy.HierarchyRelation.SUB,
					bindingOptions,
					bindingState,
					bindingContext
			);
			final var table = primaryTable.table();
			( (TableOwner) subclass ).setTable( table );
		}

		applyDiscriminatorValue( typeMetadata, subclass );

		applyCommonInformation( typeMetadata, subclass, bindingState );
		prepareAttributeBindings( subclass.getTable() );
		prepareSubclassBindings();
	}

	@Override
	public Subclass getPersistentClass() {
		return subclass;
	}

	@Override
	public Subclass getBinding() {
		return getPersistentClass();
	}

	private Subclass createSubclass() {
		final IdentifiableTypeMetadata superType = getSuperTypeMetadata();

		// we have a few cases to handle here, complicated by how Hibernate has historically modeled
		// mapped-superclass in its PersistentClass model (aka, not well) which manifests in some
		// craziness over how these PersistentClass instantiations happen

		final InheritanceType inheritanceType = superType.getHierarchy().getInheritanceType();
		if ( inheritanceType == InheritanceType.JOINED ) {
			return createJoinedSubclass( superTypeBinding.getBinding() );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			return createUnionSubclass( superTypeBinding.getBinding() );
		}

		assert inheritanceType == null || inheritanceType == InheritanceType.SINGLE_TABLE;
		return createSingleTableSubclass( superTypeBinding.getBinding() );
	}

	private UnionSubclass createUnionSubclass(IdentifiableTypeClass superTypeMapping) {
		if ( superTypeMapping instanceof PersistentClass superEntity ) {
			return new UnionSubclass( superEntity, bindingState.getMetadataBuildingContext() );
		}
		else {
			assert superTypeMapping instanceof MappedSuperclass;

			final var superEntity = resolveSuperEntityPersistentClass( superTypeMapping );
			final var binding = new UnionSubclass(
					superEntity,
					bindingState.getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeMapping );
			return binding;
		}
	}

	private JoinedSubclass createJoinedSubclass(IdentifiableTypeClass superTypeMapping) {
		final JoinedSubclass joinedSubclass;

		final var superTypePersistentClass = getSuperEntityBinding().getPersistentClass();
		if ( superTypeMapping instanceof PersistentClass superEntity ) {
			joinedSubclass = new JoinedSubclass(
					superEntity,
					bindingState.getMetadataBuildingContext()
			);
		}
		else {
			assert superTypeMapping instanceof MappedSuperclass;

			joinedSubclass = new JoinedSubclass(
					superTypePersistentClass,
					bindingState.getMetadataBuildingContext()
			);
			joinedSubclass.setSuperMappedSuperclass( (MappedSuperclass) superTypeMapping );
		}

		final var joinTableReference = TableHelper.bindPrimaryTable(
				getTypeMetadata(),
				EntityHierarchy.HierarchyRelation.SUB,
				bindingOptions,
				bindingState,
				bindingContext
		);
		joinedSubclass.setTable( joinTableReference.table() );

		final PrimaryKey primaryKey = new PrimaryKey( joinTableReference.table() );
		joinTableReference.table().setPrimaryKey( primaryKey );

		final var targetTable = superTypePersistentClass.getIdentityTable();
		if ( targetTable.getPrimaryKey() != null && targetTable.getPrimaryKey().getColumnSpan() > 0 ) {
			// we can create the foreign key immediately
			final var joinTableAnn = typeMetadata.getClassDetails().getAnnotationUsage( JoinTable.class );

			final List<AnnotationUsage<JoinColumn>> joinColumnAnns = BindingHelper.getValue(
					joinTableAnn,
					"joinColumns",
					Collections.emptyList()
			);
			final List<AnnotationUsage<JoinColumn>> inverseJoinColumnAnns = BindingHelper.getValue(
					joinTableAnn,
					"inverseJoinColumns",
					Collections.emptyList()
			);

			for ( int i = 0; i < targetTable.getPrimaryKey().getColumnSpan(); i++ ) {
				final Column targetColumn = targetTable.getPrimaryKey().getColumns().get( i );
				final Column pkColumn;
				if ( !inverseJoinColumnAnns.isEmpty() ) {
					final var joinColumnAnn = resolveMatchingJoinColumnAnn(
							inverseJoinColumnAnns,
							targetColumn,
							joinColumnAnns
					);
					pkColumn = ColumnHelper.bindColumn( joinColumnAnn, targetColumn::getName, true, false );
				}
				else {
					pkColumn = ColumnHelper.bindColumn( null, targetColumn::getName, true, false );
				}
				primaryKey.addColumn( pkColumn );
			}

			final AnnotationUsage<ForeignKey> foreignKeyAnn = BindingHelper.getValue( joinTableAnn, "foreignKey", (AnnotationUsage<ForeignKey>) null );
			final String foreignKeyName = foreignKeyAnn == null
					? ""
					: foreignKeyAnn.getString( "name" );
			final String foreignKeyDefinition = foreignKeyAnn == null
					? ""
					: foreignKeyAnn.getString( "foreignKeyDefinition" );

			final org.hibernate.mapping.ForeignKey foreignKey = targetTable.createForeignKey(
					foreignKeyName,
					primaryKey.getColumns(),
					findSuperEntityMetadata().getEntityName(),
					foreignKeyDefinition,
					targetTable.getPrimaryKey().getColumns()
			);
			foreignKey.setReferencedTable( targetTable );
		}
		else {
			throw new UnsupportedOperationException( "Delayed foreign key creation not yet implemented" );
		}

		// todo : bind foreign-key
		// todo : do same for secondary tables
		//		- in both cases we can immediately process the fk if

		return joinedSubclass;
	}

	private AnnotationUsage<JoinColumn> resolveMatchingJoinColumnAnn(
			List<AnnotationUsage<JoinColumn>> inverseJoinColumnAnns,
			Column pkColumn,
			List<AnnotationUsage<JoinColumn>> joinColumnAnns) {
		int matchPosition = -1;
		for ( int j = 0; j < inverseJoinColumnAnns.size(); j++ ) {
			final var inverseJoinColumnAnn = inverseJoinColumnAnns.get( j );
			final String name = inverseJoinColumnAnn.getString( "name" );
			if ( pkColumn.getName().equals( name ) ) {
				matchPosition = j;
				break;
			}
		}

		if ( matchPosition == -1 ) {
			throw new MappingException( "Unable to match primary key column [" + pkColumn.getName() + "] to any inverseJoinColumn - " + getTypeMetadata().getEntityName() );
		}

		return joinColumnAnns.get( matchPosition );
	}

	private SingleTableSubclass createSingleTableSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new SingleTableSubclass( superEntity, bindingState.getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntityPersistentClass( superTypeBinding );
			final SingleTableSubclass binding = new SingleTableSubclass(
					superEntity,
					bindingState.getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}
}
