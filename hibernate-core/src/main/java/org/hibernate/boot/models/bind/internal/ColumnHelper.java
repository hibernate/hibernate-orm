/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.function.Supplier;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

import static org.hibernate.internal.util.NullnessHelper.nullif;

/**
 * @author Steve Ebersole
 */
public class ColumnHelper {
	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier) {
		return bindColumn(
				annotationUsage,
				defaultNameSupplier,
				false,
				true,
				255,
				0,
				0,
				-1
		);
	}

	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault) {
		return bindColumn(
				annotationUsage,
				defaultNameSupplier,
				uniqueByDefault,
				nullableByDefault,
				255,
				0,
				0,
				-1
		);
	}

	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			int lengthByDefault,
			int precisionByDefault,
			int scaleByDefault,
			int secondPrecisionByDefault) {
		final Column result = new Column();
		result.setName( columnName( annotationUsage, defaultNameSupplier ) );

		result.setUnique( BindingHelper.getValue( annotationUsage, "unique", uniqueByDefault ) );
		result.setNullable( BindingHelper.getValue( annotationUsage, "nullable", nullableByDefault ) );
		result.setSqlType( BindingHelper.getValue( annotationUsage, "columnDefinition", (String) null ) );
		result.setLength( BindingHelper.getValue( annotationUsage, "length", lengthByDefault ) );
		result.setPrecision( BindingHelper.getValue( annotationUsage, "precision", precisionByDefault ) );
		result.setPrecision( BindingHelper.getValue( annotationUsage, "secondPrecision", secondPrecisionByDefault ) );
		result.setScale( BindingHelper.getValue( annotationUsage, "scale", scaleByDefault ) );
		return result;
	}


	public static String columnName(
			AnnotationUsage<?> columnAnnotation,
			Supplier<String> defaultNameSupplier) {
		if ( columnAnnotation == null ) {
			return defaultNameSupplier.get();
		}

		return nullif( columnAnnotation.getAttributeValue( "name" ), defaultNameSupplier );
	}

	private ColumnHelper() {
	}

	public static DiscriminatorType bindDiscriminatorColumn(
			BindingContext bindingContext,
			AnnotationUsage<DiscriminatorFormula> formulaAnn,
			BasicValue value,
			AnnotationUsage<DiscriminatorColumn> columnAnn,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final DiscriminatorType discriminatorType;
		if ( formulaAnn != null ) {
			final Formula formula = new Formula( formulaAnn.getString( "value" ) );
			value.addFormula( formula );

			discriminatorType = formulaAnn.getEnum( "discriminatorType", DiscriminatorType.STRING );
		}
		else {
			final Column column = new Column();
			value.addColumn( column, true, false );
			discriminatorType = BindingHelper.getValue( columnAnn, "discriminatorType", DiscriminatorType.STRING );

			column.setName( columnName( columnAnn, () -> "dtype" ) );
			column.setLength( (Integer) BindingHelper.getValue(
					columnAnn,
					"length",
					() -> {
						final AnnotationDescriptor<DiscriminatorColumn> descriptor;
						if ( columnAnn != null ) {
							descriptor = columnAnn.getAnnotationDescriptor();
						}
						else {
							descriptor = bindingContext.getAnnotationDescriptorRegistry().getDescriptor( DiscriminatorColumn.class );
						}
						return descriptor.getAttribute( "length" ).getAttributeMethod().getDefaultValue();
					}
			) );
			column.setSqlType( BindingHelper.getGloballyQuotedValue(
					columnAnn,
					"columnDefinition",
					() -> {
						final AnnotationDescriptor<DiscriminatorColumn> descriptor;
						if ( columnAnn != null ) {
							descriptor = columnAnn.getAnnotationDescriptor();
						}
						else {
							descriptor = bindingContext.getAnnotationDescriptorRegistry().getDescriptor( DiscriminatorColumn.class );
						}
						return (String) descriptor.getAttribute( "columnDefinition" ).getAttributeMethod().getDefaultValue();
					},
					bindingOptions,
					bindingState
			) );
			applyOptions( column, columnAnn );
		}
		return discriminatorType;
	}

	private static void applyOptions(Column column, AnnotationUsage<?> columnAnn) {
		if ( columnAnn != null ) {
			final String options = columnAnn.getString( "options" );
			if ( StringHelper.isNotEmpty( options ) ) {
				// todo : see https://hibernate.atlassian.net/browse/HHH-17449
//				table.setOptions( options );
				throw new UnsupportedOperationException( "Not yet implemented" );
			}
		}
	}
}
