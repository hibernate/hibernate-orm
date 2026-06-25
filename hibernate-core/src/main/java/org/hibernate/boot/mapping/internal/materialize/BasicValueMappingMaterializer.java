/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.binders.BasicValueBinder;
import org.hibernate.boot.mapping.internal.binders.ColumnBinder;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.ComponentMemberBinding;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;

import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.bindImplicitJavaType;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.applyColumnTransformer;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.processSelectable;
import static org.hibernate.boot.mapping.internal.binders.BasicValueBinder.bindJavaType;
import static org.hibernate.boot.mapping.internal.binders.BasicValueBinder.bindJdbcType;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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

		final var selectable = processSelectable( attributeBinding, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		final var column = selectable.column();
		applyBasicOptionality( member, attributeBinding.resolvedType(), property, column );
		applyBasicFetch( member, property );
		property.setLob( member.hasDirectAnnotationUsage( Lob.class ) );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute(
						member,
						attributeBinding.resolvedType(),
						bindingContext
				),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		new AttributeOptionsMappingMaterializer().materializeOptions( attributeBinding, property, basicValue );

		return basicValue;
	}

	public void materializeVersionBasicValue(
			MemberDetails member,
			BasicValueIntent basicValueIntent,
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

		final Column column = processSelectable( basicValueIntent, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext )
				.requireColumn( property.getName() );
		column.setNullable( false );
	}

	public void materializeTenantIdBasicValue(
			MemberDetails member,
			BasicValueIntent basicValueIntent,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), primaryTable );
		property.setValue( basicValue );

		processSelectable( basicValueIntent, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member, bindingContext ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	public MaterializedBasicValue createComponentMemberBasicValue(
			ComponentSource source,
			ComponentMemberBinding componentMember,
			Property property,
			Table table,
			List<String> columnNamingPatterns,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		property.setValue( basicValue );

		final MemberDetails member = componentMember.member();
		final BasicValueIntent basicValueIntent = componentMember.basicValueIntent();
		if ( basicValueIntent.isFormula() ) {
			basicValue.addFormula( new org.hibernate.mapping.Formula( basicValueIntent.formulaExpression() ) );
			property.setOptional( true );
			property.setInsertable( false );
			property.setUpdatable( false );
			BasicValueBinder.bindBasicValue(
					BasicValueSource.embeddableMember( member, basicValueIntent.conversion() ),
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);
			return new MaterializedBasicValue( basicValue, null );
		}

			final Column column = bindComponentMemberColumn(
					() -> implicitBasicColumnName( source, componentMember, bindingState, bindingContext ),
					property,
					basicValue,
					basicValueIntent,
				columnNamingPatterns,
				uniqueByDefault,
				nullableByDefault,
				basicValueIntent.insertable(),
				updatable && basicValueIntent.updatable()
		);
		applyBasicOptionality( member, componentMember.type(), property, column );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.embeddableMember( member, componentMember.type(), basicValueIntent.conversion() ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		return new MaterializedBasicValue( basicValue, column );
	}

	private Column bindComponentMemberColumn(
			Supplier<String> implicitName,
			Property property,
			BasicValue basicValue,
			BasicValueIntent basicValueIntent,
			List<String> columnNamingPatterns,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean insertable,
			boolean updatable) {
		final Column column = ColumnBinder.bindColumn(
				basicValueIntent.columnSource(),
				implicitName,
				uniqueByDefault,
				nullableByDefault
			);
			column.setName( applyColumnNamingPatterns( column.getName(), columnNamingPatterns ) );
			applyColumnTransformer( basicValueIntent, property, column );
			basicValue.addColumn( column, insertable, updatable );
		basicValue.getTable().addColumn( column );
		return column;
	}

	private static String implicitBasicColumnName(
			ComponentSource source,
			ComponentMemberBinding member,
			BindingState bindingState,
			BindingContext bindingContext) {
		return bindingContext.getImplicitNamingStrategy()
				.determineBasicColumnName( new ImplicitBasicColumnNameSource() {
					@Override
					public AttributePath getAttributePath() {
						return member.namingPath();
					}

					@Override
					public boolean isCollectionElement() {
						return false;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private static String applyColumnNamingPatterns(String name, List<String> patterns) {
		if ( patterns.isEmpty() ) {
			return name;
		}

		String result = name;
		for ( int i = patterns.size() - 1; i >= 0; i-- ) {
			final String pattern = patterns.get( i );
			if ( isNotEmpty( pattern ) ) {
				result = String.format( Locale.ROOT, pattern, result );
			}
		}
		return result;
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

	private static void applyBasicOptionality(MemberDetails member, TypeDetails memberType, Property property, Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = memberType.getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		property.setOptional( optionalByType && optionalByBasic && optionalByColumn );
	}

	/// Result of materializing a basic value from a value intent.
	///
	/// Formula-valued basics have no materialized [Column], so [#column()] is
	/// nullable.
	public record MaterializedBasicValue(BasicValue value, Column column) {
	}
}
