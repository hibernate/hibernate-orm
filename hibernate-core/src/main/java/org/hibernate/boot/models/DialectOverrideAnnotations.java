package org.hibernate.boot.models;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.FetchProfileOverrides;
import org.hibernate.boot.models.annotations.internal.FetchProfileOverrideAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfileOverridesAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenCheckAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenChecksAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenColumnDefaultAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenColumnDefaultsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenDiscriminatorFormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenDiscriminatorFormulasAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFilterDefOverridesAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFilterDefsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFilterOverridesAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenFormulasAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenGeneratedColumnAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenGeneratedColumnsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenJoinFormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenJoinFormulasAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLDeleteAllsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLDeletesAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLInsertsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLOrderAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLOrdersAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLRestrictionsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLSelectAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLSelectsAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.OverriddenSQLUpdatesAnnotation;
import org.hibernate.boot.models.annotations.internal.OverrideVersionAnnotation;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.CompleteAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface DialectOverrideAnnotations {
	CompleteAnnotationDescriptor<DialectOverride.Checks, OverriddenChecksAnnotation> DIALECT_OVERRIDE_CHECKS = new CompleteAnnotationDescriptor<>(
			DialectOverride.Checks.class,
			OverriddenChecksAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.Check, OverriddenCheckAnnotation> DIALECT_OVERRIDE_CHECK = new CompleteAnnotationDescriptor<>(
			DialectOverride.Check.class,
			OverriddenCheckAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_CHECKS
	);
	CompleteAnnotationDescriptor<DialectOverride.ColumnDefaults, OverriddenColumnDefaultsAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULTS = new CompleteAnnotationDescriptor<>(
			DialectOverride.ColumnDefaults.class,
			OverriddenColumnDefaultsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.ColumnDefault, OverriddenColumnDefaultAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULT = new CompleteAnnotationDescriptor<>(
			DialectOverride.ColumnDefault.class,
			OverriddenColumnDefaultAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_COLUMN_DEFAULTS
	);
	CompleteAnnotationDescriptor<DialectOverride.GeneratedColumns, OverriddenGeneratedColumnsAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMNS = new CompleteAnnotationDescriptor<>(
			DialectOverride.GeneratedColumns.class,
			OverriddenGeneratedColumnsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.GeneratedColumn, OverriddenGeneratedColumnAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMN = new CompleteAnnotationDescriptor<>(
			DialectOverride.GeneratedColumn.class,
			OverriddenGeneratedColumnAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_GENERATED_COLUMNS
	);
	CompleteAnnotationDescriptor<DialectOverride.DiscriminatorFormulas, OverriddenDiscriminatorFormulasAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS = new CompleteAnnotationDescriptor<>(
			DialectOverride.DiscriminatorFormulas.class,
			OverriddenDiscriminatorFormulasAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.DiscriminatorFormula, OverriddenDiscriminatorFormulaAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA = new CompleteAnnotationDescriptor<>(
			DialectOverride.DiscriminatorFormula.class,
			OverriddenDiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS
	);
	CompleteAnnotationDescriptor<FetchProfileOverrides, FetchProfileOverridesAnnotation> FETCH_PROFILE_OVERRIDES = new CompleteAnnotationDescriptor<>(
			FetchProfileOverrides.class,
			FetchProfileOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<FetchProfileOverride, FetchProfileOverrideAnnotation> FETCH_PROFILE_OVERRIDE = new CompleteAnnotationDescriptor<>(
			FetchProfileOverride.class,
			FetchProfileOverrideAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			FETCH_PROFILE_OVERRIDES
	);
	CompleteAnnotationDescriptor<DialectOverride.FilterOverrides, OverriddenFilterOverridesAnnotation> DIALECT_OVERRIDE_FILTER_OVERRIDES = new CompleteAnnotationDescriptor<>(
			DialectOverride.FilterOverrides.class,
			OverriddenFilterOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.Filters, OverriddenFiltersAnnotation> DIALECT_OVERRIDE_FILTERS = new CompleteAnnotationDescriptor<DialectOverride.Filters, OverriddenFiltersAnnotation>(
			DialectOverride.Filters.class,
			OverriddenFiltersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_OVERRIDES
	);
	CompleteAnnotationDescriptor<DialectOverride.FilterDefOverrides, OverriddenFilterDefOverridesAnnotation> DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES = new CompleteAnnotationDescriptor<>(
			DialectOverride.FilterDefOverrides.class,
			OverriddenFilterDefOverridesAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.FilterDefs, OverriddenFilterDefsAnnotation> DIALECT_OVERRIDE_FILTER_DEFS = new CompleteAnnotationDescriptor<>(
			DialectOverride.FilterDefs.class,
			OverriddenFilterDefsAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES
	);
	CompleteAnnotationDescriptor<DialectOverride.Formulas, OverriddenFormulasAnnotation> DIALECT_OVERRIDE_FORMULAS = new CompleteAnnotationDescriptor<>(
			DialectOverride.Formulas.class,
			OverriddenFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.Formula, OverriddenFormulaAnnotation> DIALECT_OVERRIDE_FORMULA = new CompleteAnnotationDescriptor<>(
			DialectOverride.Formula.class,
			OverriddenFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_FORMULAS
	);
	CompleteAnnotationDescriptor<DialectOverride.JoinFormulas, OverriddenJoinFormulasAnnotation> DIALECT_OVERRIDE_JOIN_FORMULAS = new CompleteAnnotationDescriptor<>(
			DialectOverride.JoinFormulas.class,
			OverriddenJoinFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.JoinFormula, OverriddenJoinFormulaAnnotation> DIALECT_OVERRIDE_JOIN_FORMULA = new CompleteAnnotationDescriptor<>(
			DialectOverride.JoinFormula.class,
			OverriddenJoinFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_JOIN_FORMULAS
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLInserts, OverriddenSQLInsertsAnnotation> DIALECT_OVERRIDE_SQL_INSERTS = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLInserts.class,
			OverriddenSQLInsertsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLInsert, OverriddenSQLInsertAnnotation> DIALECT_OVERRIDE_SQL_INSERT = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLInsert.class,
			OverriddenSQLInsertAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_INSERTS
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLUpdates, OverriddenSQLUpdatesAnnotation> DIALECT_OVERRIDE_SQL_UPDATES = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLUpdates.class,
			OverriddenSQLUpdatesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLUpdate, OverriddenSQLUpdateAnnotation> DIALECT_OVERRIDE_SQL_UPDATE = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLUpdate.class,
			OverriddenSQLUpdateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_UPDATES
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLDeletes, OverriddenSQLDeletesAnnotation> DIALECT_OVERRIDE_SQL_DELETES = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLDeletes.class,
			OverriddenSQLDeletesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLDelete, OverriddenSQLDeleteAnnotation> DIALECT_OVERRIDE_SQL_DELETE = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLDelete.class,
			OverriddenSQLDeleteAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETES
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLDeleteAlls, OverriddenSQLDeleteAllsAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALLS = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLDeleteAlls.class,
			OverriddenSQLDeleteAllsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLDeleteAll, OverriddenSQLDeleteAllAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALL = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLDeleteAll.class,
			OverriddenSQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETE_ALLS
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLOrders, OverriddenSQLOrdersAnnotation> DIALECT_OVERRIDE_SQL_ORDERS = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLOrders.class,
			OverriddenSQLOrdersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLOrder, OverriddenSQLOrderAnnotation> DIALECT_OVERRIDE_SQL_ORDER = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLOrder.class,
			OverriddenSQLOrderAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_SQL_ORDERS
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLRestrictions, OverriddenSQLRestrictionsAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTIONS = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLRestrictions.class,
			OverriddenSQLRestrictionsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLRestriction, OverriddenSQLRestrictionAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTION = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLRestriction.class,
			OverriddenSQLRestrictionAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_RESTRICTIONS
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLSelects, OverriddenSQLSelectsAnnotation> DIALECT_OVERRIDE_SQL_SELECTS = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLSelects.class,
			OverriddenSQLSelectsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DialectOverride.SQLSelect, OverriddenSQLSelectAnnotation> DIALECT_OVERRIDE_SQL_SELECT = new CompleteAnnotationDescriptor<>(
			DialectOverride.SQLSelect.class,
			OverriddenSQLSelectAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_SELECTS
	);
	CompleteAnnotationDescriptor<DialectOverride.Version, OverrideVersionAnnotation> DIALECT_OVERRIDE_VERSION = new CompleteAnnotationDescriptor<>(
			DialectOverride.Version.class,
			OverrideVersionAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( DialectOverrideAnnotations.class, consumer );
	}
}
