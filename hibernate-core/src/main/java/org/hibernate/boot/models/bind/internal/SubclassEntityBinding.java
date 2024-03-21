/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;

import jakarta.persistence.InheritanceType;

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

		applyTable(
				typeMetadata,
				subclass,
				bindingOptions,
				bindingState,
				bindingContext
		);

		TableHelper.bindSecondaryTables(
				this,
				bindingOptions,
				bindingState,
				bindingContext
		);

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
	public RootEntityBinding getRootEntityBinding() {
		return getSuperEntityBinding().getRootEntityBinding();
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
		final UnionSubclass unionSubclass;
		if ( superTypeMapping instanceof PersistentClass superEntity ) {
			unionSubclass = new UnionSubclass( superEntity, bindingState.getMetadataBuildingContext() );
		}
		else {
			assert superTypeMapping instanceof MappedSuperclass;

			final var superEntity = resolveSuperEntityPersistentClass( superTypeMapping );
			unionSubclass = new UnionSubclass(
					superEntity,
					bindingState.getMetadataBuildingContext()
			);
			unionSubclass.setSuperMappedSuperclass( (MappedSuperclass) superTypeMapping );
		}

		return unionSubclass;
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

		getRootEntityBinding().getIdentifierBinding().whenResolved( new JoinedSubclassKeyHandler(
				getTypeMetadata().getClassDetails(),
				joinedSubclass,
				bindingState.getMetadataBuildingContext()
		) );

		return joinedSubclass;
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

	protected void applyTable(
			EntityTypeMetadata typeMetadata,
			Subclass subclass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( subclass instanceof UnionSubclass unionSubclass ) {
			final var unionTableReference = TableHelper.bindPrimaryTable(
					getTypeMetadata(),
					EntityHierarchy.HierarchyRelation.SUB,
					bindingOptions,
					bindingState,
					bindingContext
			);
			unionSubclass.setTable( unionTableReference.table() );

			final PrimaryKey primaryKey = new PrimaryKey( unionTableReference.table() );
			unionTableReference.table().setPrimaryKey( primaryKey );
		}
		else if ( subclass instanceof JoinedSubclass joinedSubclass ) {

		}
	}
}
