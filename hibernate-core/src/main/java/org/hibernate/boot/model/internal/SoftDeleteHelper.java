/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SoftDeleteMappingImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import static org.hibernate.internal.util.StringHelper.coalesce;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;

/**
 * Helper for dealing with {@link org.hibernate.annotations.SoftDelete}
 *
 * @author Steve Ebersole
 */
public class SoftDeleteHelper {
	/**
	 * Creates and binds the column and value for modeling the soft-delete in the database
	 *
	 * @param softDeleteConfig The SoftDelete annotation
	 * @param target The thing which is to be soft-deleted
	 * @param table The table to which the soft-delete should be applied
	 * @param context The processing context for access to needed info and services
	 */
	public static void bindSoftDeleteIndicator(
			SoftDelete softDeleteConfig,
			SoftDeletable target,
			Table table,
			MetadataBuildingContext context) {
		assert softDeleteConfig != null;

		final BasicValue softDeleteIndicatorValue = createSoftDeleteIndicatorValue( softDeleteConfig, table, context );
		final Column softDeleteIndicatorColumn = createSoftDeleteIndicatorColumn(
				softDeleteConfig,
				softDeleteIndicatorValue,
				context
		);
		table.addColumn( softDeleteIndicatorColumn );
		target.enableSoftDelete( softDeleteIndicatorColumn );
	}

	private static BasicValue createSoftDeleteIndicatorValue(
			SoftDelete softDeleteConfig,
			Table table,
			MetadataBuildingContext context) {
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				softDeleteConfig.converter(),
				context.getBootstrapContext().getClassmateContext()
		);

		final BasicValue softDeleteIndicatorValue = new BasicValue( context, table );
		softDeleteIndicatorValue.makeSoftDelete( softDeleteConfig.strategy() );
		softDeleteIndicatorValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		softDeleteIndicatorValue.setImplicitJavaTypeAccess( (typeConfiguration) -> converterDescriptor.getRelationalValueResolvedType().getErasedType() );
		return softDeleteIndicatorValue;
	}

	private static Column createSoftDeleteIndicatorColumn(
			SoftDelete softDeleteConfig,
			BasicValue softDeleteIndicatorValue,
			MetadataBuildingContext context) {
		final Column softDeleteColumn = new Column();

		applyColumnName( softDeleteColumn, softDeleteConfig, context );

		softDeleteColumn.setLength( 1 );
		softDeleteColumn.setNullable( false );
		softDeleteColumn.setUnique( false );
		softDeleteColumn.setOptions( softDeleteConfig.options() );
		if ( StringHelper.isEmpty( softDeleteConfig.comment() ) ) {
			softDeleteColumn.setComment( "Soft-delete indicator" );
		}
		else {
			softDeleteColumn.setComment( softDeleteConfig.comment() );
		}

		softDeleteColumn.setValue( softDeleteIndicatorValue );
		softDeleteIndicatorValue.addColumn( softDeleteColumn );

		return softDeleteColumn;
	}

	private static void applyColumnName(
			Column softDeleteColumn,
			SoftDelete softDeleteConfig,
			MetadataBuildingContext context) {
		final Database database = context.getMetadataCollector().getDatabase();
		final PhysicalNamingStrategy namingStrategy = context.getBuildingOptions().getPhysicalNamingStrategy();
		final String logicalColumnName = coalesce(
				softDeleteConfig.strategy().getDefaultColumnName(),
				softDeleteConfig.columnName()
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}

	public static SoftDeleteMappingImpl resolveSoftDeleteMapping(
			SoftDeletableModelPart softDeletableModelPart,
			SoftDeletable bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		return resolveSoftDeleteMapping(
				softDeletableModelPart,
				bootMapping,
				tableName,
				creationProcess.getCreationContext().getDialect()
		);
	}

	public static SoftDeleteMappingImpl resolveSoftDeleteMapping(
			SoftDeletableModelPart softDeletableModelPart,
			SoftDeletable bootMapping,
			String tableName,
			Dialect dialect) {
		final Column softDeleteColumn = bootMapping.getSoftDeleteColumn();
		if ( softDeleteColumn == null ) {
			return null;
		}

		final BasicValue columnValue = (BasicValue) softDeleteColumn.getValue();
		final BasicValue.Resolution<?> resolution = columnValue.resolve();
		//noinspection unchecked
		final BasicValueConverter<Boolean, Object> converter = (BasicValueConverter<Boolean, Object>) resolution.getValueConverter();
		//noinspection unchecked
		final JdbcLiteralFormatter<Object> literalFormatter = resolution.getJdbcMapping().getJdbcLiteralFormatter();

		final Object deletedLiteralValue;
		final Object nonDeletedLiteralValue;
		if ( converter == null ) {
			// the database column is BIT or BOOLEAN : pass-thru
			deletedLiteralValue = true;
			nonDeletedLiteralValue = false;
		}
		else {
			deletedLiteralValue = converter.toRelationalValue( true );
			nonDeletedLiteralValue = converter.toRelationalValue( false );
		}

		return new SoftDeleteMappingImpl(
				softDeletableModelPart,
				softDeleteColumn.getName(),
				tableName,
				deletedLiteralValue,
				literalFormatter.toJdbcLiteral( deletedLiteralValue, dialect, null ),
				nonDeletedLiteralValue,
				literalFormatter.toJdbcLiteral( nonDeletedLiteralValue, dialect, null ),
				resolution.getJdbcMapping()
		);
	}

	/**
	 * Create a SQL AST Predicate for restricting matches to non-deleted rows
	 *
	 * @param tableReference The table reference for the table containing the soft-delete column
	 * @param softDeleteMapping The soft-delete mapping
	 */
	public static Predicate createNonSoftDeletedRestriction(
			TableReference tableReference,
			SoftDeleteMapping softDeleteMapping) {
		final ColumnReference softDeleteColumn = new ColumnReference( tableReference, softDeleteMapping );
		final JdbcLiteral<?> notDeletedLiteral = new JdbcLiteral<>(
				softDeleteMapping.getNonDeletedLiteralValue(),
				softDeleteMapping.getJdbcMapping()
		);
		return new ComparisonPredicate( softDeleteColumn, EQUAL, notDeletedLiteral );
	}

	/**
	 * Create a SQL AST Predicate for restricting matches to non-deleted rows
	 *
	 * @param tableReference The table reference for the table containing the soft-delete column
	 * @param softDeleteMapping The soft-delete mapping
	 */
	public static Predicate createNonSoftDeletedRestriction(
			TableReference tableReference,
			SoftDeleteMapping softDeleteMapping,
			SqlExpressionResolver expressionResolver) {
		final Expression softDeleteColumn = expressionResolver.resolveSqlExpression( tableReference, softDeleteMapping );
		final JdbcLiteral<?> notDeletedLiteral = new JdbcLiteral<>(
				softDeleteMapping.getNonDeletedLiteralValue(),
				softDeleteMapping.getJdbcMapping()
		);
		return new ComparisonPredicate( softDeleteColumn, EQUAL, notDeletedLiteral );
	}

	/**
	 * Create a SQL AST Assignment for setting the soft-delete column to its
	 * deleted indicate value
	 *
	 * @param tableReference The table reference for the table containing the soft-delete column
	 * @param softDeleteMapping The soft-delete mapping
	 */
	public static Assignment createSoftDeleteAssignment(
			TableReference tableReference,
			SoftDeleteMapping softDeleteMapping) {
		final ColumnReference softDeleteColumn = new ColumnReference( tableReference, softDeleteMapping );
		final JdbcLiteral<?> softDeleteIndicator = new JdbcLiteral<>(
				softDeleteMapping.getDeletedLiteralValue(),
				softDeleteMapping.getJdbcMapping()
		);
		return new Assignment( softDeleteColumn, softDeleteIndicator );
	}
}
