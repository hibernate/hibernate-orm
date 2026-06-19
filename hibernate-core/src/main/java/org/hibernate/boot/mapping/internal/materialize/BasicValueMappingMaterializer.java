/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.binders.BasicValueBinder;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;

import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.bindImplicitJavaType;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.processColumn;
import static org.hibernate.boot.mapping.internal.binders.BasicValueBinder.bindJavaType;
import static org.hibernate.boot.mapping.internal.binders.BasicValueBinder.bindJdbcType;

/// Materializes legacy `BasicValue` mapping objects for simple basic-shaped
/// attributes.
///
/// The methods intentionally mirror the existing binder-specific behavior
/// rather than normalizing all basic values into one path.  Normal attributes,
/// version attributes, and tenant-id attributes currently apply different
/// secondary side effects around the same mapping object type.
///
/// @since 9.0
/// @author Steve Ebersole
public class BasicValueMappingMaterializer {
	public BasicValue createAttributeBasicValue(
			AttributeBindingView attributeBinding,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MemberDetails member = attributeBinding.member();
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );

		final var column = processColumn( attributeBinding, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		applyBasicOptionality( member, property, column );
		applyBasicFetch( member, property );
		property.setLob( member.hasDirectAnnotationUsage( Lob.class ) );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		new AttributeOptionsMappingMaterializer().materializeOptions( attributeBinding, property, basicValue );

		return basicValue;
	}

	public BasicValue createVersionBasicValue(
			MemberDetails member,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), primaryTable );
		property.setValue( basicValue );

		bindImplicitJavaType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( member, property, basicValue, bindingOptions, bindingState, bindingContext );

		final Column column = processColumn( member, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		column.setNullable( false );
		return basicValue;
	}

	public BasicValue createTenantIdBasicValue(
			MemberDetails member,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), primaryTable );
		property.setValue( basicValue );

		processColumn( member, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		return basicValue;
	}

	private static void applyBasicOptionality(MemberDetails member, Property property, Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = member.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		property.setOptional( optionalByType && optionalByBasic && optionalByColumn );
	}

	private static void applyBasicFetch(MemberDetails member, Property property) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		property.setLazy( basic != null && basic.fetch() == FetchType.LAZY );
	}
}
