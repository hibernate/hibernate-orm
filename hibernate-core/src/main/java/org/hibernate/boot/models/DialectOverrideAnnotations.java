/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface DialectOverrideAnnotations {
	OrmAnnotationDescriptor<DialectOverride.Checks, OverriddenChecksAnnotation> DIALECT_OVERRIDE_CHECKS = new OrmAnnotationDescriptor<>(
			DialectOverride.Checks.class,
			OverriddenChecksAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.Check, OverriddenCheckAnnotation> DIALECT_OVERRIDE_CHECK = new OrmAnnotationDescriptor<>(
			DialectOverride.Check.class,
			OverriddenCheckAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_CHECKS
	);
	OrmAnnotationDescriptor<DialectOverride.ColumnDefaults, OverriddenColumnDefaultsAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULTS = new OrmAnnotationDescriptor<>(
			DialectOverride.ColumnDefaults.class,
			OverriddenColumnDefaultsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.ColumnDefault, OverriddenColumnDefaultAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULT = new OrmAnnotationDescriptor<>(
			DialectOverride.ColumnDefault.class,
			OverriddenColumnDefaultAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_COLUMN_DEFAULTS
	);
	OrmAnnotationDescriptor<DialectOverride.GeneratedColumns, OverriddenGeneratedColumnsAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMNS = new OrmAnnotationDescriptor<>(
			DialectOverride.GeneratedColumns.class,
			OverriddenGeneratedColumnsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.GeneratedColumn, OverriddenGeneratedColumnAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMN = new OrmAnnotationDescriptor<>(
			DialectOverride.GeneratedColumn.class,
			OverriddenGeneratedColumnAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_GENERATED_COLUMNS
	);
	OrmAnnotationDescriptor<DialectOverride.DiscriminatorFormulas, OverriddenDiscriminatorFormulasAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS = new OrmAnnotationDescriptor<>(
			DialectOverride.DiscriminatorFormulas.class,
			OverriddenDiscriminatorFormulasAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.DiscriminatorFormula, OverriddenDiscriminatorFormulaAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA = new OrmAnnotationDescriptor<>(
			DialectOverride.DiscriminatorFormula.class,
			OverriddenDiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS
	);
	OrmAnnotationDescriptor<FetchProfileOverrides, FetchProfileOverridesAnnotation> FETCH_PROFILE_OVERRIDES = new OrmAnnotationDescriptor<>(
			FetchProfileOverrides.class,
			FetchProfileOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<FetchProfileOverride, FetchProfileOverrideAnnotation> FETCH_PROFILE_OVERRIDE = new OrmAnnotationDescriptor<>(
			FetchProfileOverride.class,
			FetchProfileOverrideAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			FETCH_PROFILE_OVERRIDES
	);
	OrmAnnotationDescriptor<DialectOverride.FilterOverrides, OverriddenFilterOverridesAnnotation> DIALECT_OVERRIDE_FILTER_OVERRIDES = new OrmAnnotationDescriptor<>(
			DialectOverride.FilterOverrides.class,
			OverriddenFilterOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.Filters, OverriddenFiltersAnnotation> DIALECT_OVERRIDE_FILTERS = new OrmAnnotationDescriptor<DialectOverride.Filters, OverriddenFiltersAnnotation>(
			DialectOverride.Filters.class,
			OverriddenFiltersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_OVERRIDES
	);
	OrmAnnotationDescriptor<DialectOverride.FilterDefOverrides, OverriddenFilterDefOverridesAnnotation> DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES = new OrmAnnotationDescriptor<>(
			DialectOverride.FilterDefOverrides.class,
			OverriddenFilterDefOverridesAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.FilterDefs, OverriddenFilterDefsAnnotation> DIALECT_OVERRIDE_FILTER_DEFS = new OrmAnnotationDescriptor<>(
			DialectOverride.FilterDefs.class,
			OverriddenFilterDefsAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES
	);
	OrmAnnotationDescriptor<DialectOverride.Formulas, OverriddenFormulasAnnotation> DIALECT_OVERRIDE_FORMULAS = new OrmAnnotationDescriptor<>(
			DialectOverride.Formulas.class,
			OverriddenFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.Formula, OverriddenFormulaAnnotation> DIALECT_OVERRIDE_FORMULA = new OrmAnnotationDescriptor<>(
			DialectOverride.Formula.class,
			OverriddenFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_FORMULAS
	);
	OrmAnnotationDescriptor<DialectOverride.JoinFormulas, OverriddenJoinFormulasAnnotation> DIALECT_OVERRIDE_JOIN_FORMULAS = new OrmAnnotationDescriptor<>(
			DialectOverride.JoinFormulas.class,
			OverriddenJoinFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.JoinFormula, OverriddenJoinFormulaAnnotation> DIALECT_OVERRIDE_JOIN_FORMULA = new OrmAnnotationDescriptor<>(
			DialectOverride.JoinFormula.class,
			OverriddenJoinFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_JOIN_FORMULAS
	);
	OrmAnnotationDescriptor<DialectOverride.SQLInserts, OverriddenSQLInsertsAnnotation> DIALECT_OVERRIDE_SQL_INSERTS = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLInserts.class,
			OverriddenSQLInsertsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLInsert, OverriddenSQLInsertAnnotation> DIALECT_OVERRIDE_SQL_INSERT = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLInsert.class,
			OverriddenSQLInsertAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_INSERTS
	);
	OrmAnnotationDescriptor<DialectOverride.SQLUpdates, OverriddenSQLUpdatesAnnotation> DIALECT_OVERRIDE_SQL_UPDATES = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLUpdates.class,
			OverriddenSQLUpdatesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLUpdate, OverriddenSQLUpdateAnnotation> DIALECT_OVERRIDE_SQL_UPDATE = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLUpdate.class,
			OverriddenSQLUpdateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_UPDATES
	);
	OrmAnnotationDescriptor<DialectOverride.SQLDeletes, OverriddenSQLDeletesAnnotation> DIALECT_OVERRIDE_SQL_DELETES = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLDeletes.class,
			OverriddenSQLDeletesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLDelete, OverriddenSQLDeleteAnnotation> DIALECT_OVERRIDE_SQL_DELETE = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLDelete.class,
			OverriddenSQLDeleteAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETES
	);
	OrmAnnotationDescriptor<DialectOverride.SQLDeleteAlls, OverriddenSQLDeleteAllsAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALLS = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLDeleteAlls.class,
			OverriddenSQLDeleteAllsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLDeleteAll, OverriddenSQLDeleteAllAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALL = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLDeleteAll.class,
			OverriddenSQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETE_ALLS
	);
	OrmAnnotationDescriptor<DialectOverride.SQLOrders, OverriddenSQLOrdersAnnotation> DIALECT_OVERRIDE_SQL_ORDERS = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLOrders.class,
			OverriddenSQLOrdersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLOrder, OverriddenSQLOrderAnnotation> DIALECT_OVERRIDE_SQL_ORDER = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLOrder.class,
			OverriddenSQLOrderAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_SQL_ORDERS
	);
	OrmAnnotationDescriptor<DialectOverride.SQLRestrictions, OverriddenSQLRestrictionsAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTIONS = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLRestrictions.class,
			OverriddenSQLRestrictionsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLRestriction, OverriddenSQLRestrictionAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTION = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLRestriction.class,
			OverriddenSQLRestrictionAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_RESTRICTIONS
	);
	OrmAnnotationDescriptor<DialectOverride.SQLSelects, OverriddenSQLSelectsAnnotation> DIALECT_OVERRIDE_SQL_SELECTS = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLSelects.class,
			OverriddenSQLSelectsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DialectOverride.SQLSelect, OverriddenSQLSelectAnnotation> DIALECT_OVERRIDE_SQL_SELECT = new OrmAnnotationDescriptor<>(
			DialectOverride.SQLSelect.class,
			OverriddenSQLSelectAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_SELECTS
	);
	OrmAnnotationDescriptor<DialectOverride.Version, OverrideVersionAnnotation> DIALECT_OVERRIDE_VERSION = new OrmAnnotationDescriptor<>(
			DialectOverride.Version.class,
			OverrideVersionAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( DialectOverrideAnnotations.class, consumer );
	}
}
