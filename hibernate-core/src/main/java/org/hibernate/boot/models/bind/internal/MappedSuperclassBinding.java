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
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassBinding extends IdentifiableTypeBinding {
	private final MappedSuperclass mappedSuperclass;

	public MappedSuperclassBinding(
			MappedSuperclassTypeMetadata type,
			IdentifiableTypeBinding superTypeBinding,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingOptions options,
			BindingState state,
			BindingContext bindingContext) {
		super( type, superTypeBinding, hierarchyRelation, options, state, bindingContext );

		final EntityBinding superEntityBinding = getSuperEntityBinding();
		final MappedSuperclass superMappedSuper;
		final PersistentClass superEntity;
		if ( superTypeBinding == superEntityBinding && superTypeBinding != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinding.getPersistentClass();
		}
		else if ( superTypeBinding != null ) {
			superMappedSuper = (MappedSuperclass) superTypeBinding.getBinding();
			superEntity = null;
		}
		else if ( superEntityBinding != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinding.getPersistentClass();
		}
		else {
			superMappedSuper = null;
			superEntity = null;
		}

		this.mappedSuperclass = new MappedSuperclass( superMappedSuper, superEntity, getTable() );
		state.registerTypeBinding( type, this );

		state.getMetadataBuildingContext().getMetadataCollector().addImport(
				StringHelper.unqualify( type.getClassDetails().getClassName() ),
				type.getClassDetails().getClassName()
		);
	}

	public MappedSuperclass getMappedSuperclass() {
		return mappedSuperclass;
	}

	@Override
	public MappedSuperclass getBinding() {
		return getMappedSuperclass();
	}

	@Override
	protected AttributeBinding createAttributeBinding(AttributeMetadata attributeMetadata, Table primaryTable) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	Table getTable() {
		final var superEntityBinder = getSuperEntityBinding();
		if ( superEntityBinder == null ) {
			return null;
		}

		return superEntityBinder.getPersistentClass().getTable();
	}
}
