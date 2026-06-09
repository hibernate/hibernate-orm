/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.internal.sources.AnySource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

/// Binds a singular Hibernate `@Any` attribute.
///
/// The heavy lifting is delegated to [AnyValueBinder] so the same two-column
/// discriminated association value can later be reused as a `@ManyToAny`
/// collection element.  When singular `@Any` uses `@JoinTable`, this binder
/// creates the association-table [Join], defers its owner key through the
/// table-key phase, and binds the discriminator/key pair on that association
/// table.
///
/// @since 9.0
/// @author Steve Ebersole
class AnyAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final ModelBinders modelBinders;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	AnyAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Any bind(Property property, Table table) {
		final AnySource source = AnySource.create( attributeMetadata.getMember(), bindingContext, bindingState );
		final Table valueTable = source.joinTable() == null ? table : bindAssociationTable( source, table );
		final Any value = new AnyValueBinder(
				bindingOptions,
				bindingState,
				bindingContext
		).bind( source, attributeMetadata.getName(), valueTable );
		property.setOptional( source.optional() );
		property.setCascade( source.cascades() );
		return value;
	}

	private Table bindAssociationTable(AnySource source, Table ownerTable) {
		final JoinTable joinTable = source.joinTable();
		if ( StringHelper.isEmpty( joinTable.name() ) ) {
			// todo (any) : support implicit @Any association-table names without a single target entity
			throw new UnsupportedOperationException(
					"Implicit @Any @JoinTable names are not yet implemented - " + source.member().getName()
			);
		}

		final Table associationTable = modelBinders.getTableBinder()
				.bindAssociationTable(
						resolveOwnerEntityType(),
						ownerTable,
						attributeMetadata.getName(),
						resolveOwnerEntityType(),
						ownerTable,
						joinTable
				)
				.binding();

		final Join join = new Join();
		join.setTable( associationTable );
		join.setPersistentClass( ownerBinding );
		join.setOptional( source.optional() );
		join.setInverse( false );
		ownerBinding.addJoin( join );

		final IdentifierBinding ownerIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( ownerIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for @Any association table owner - "
							+ ownerType.getClassDetails().getClassName()
			);
		}

		final List<JoinColumn> ownerJoinColumns = source.ownerJoinColumns();
		if ( !ownerJoinColumns.isEmpty() && ownerJoinColumns.size() != ownerIdentifierBinding.columns().size() ) {
			throw new MappingException(
					"@Any association table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}

		bindingState.addAssociationTableBinding( new AssociationTableBinding(
				join,
				ownerJoinColumns,
				ForeignKeySource.from( joinTable )
		) );
		return associationTable;
	}

	private EntityTypeMetadata resolveOwnerEntityType() {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}
}
