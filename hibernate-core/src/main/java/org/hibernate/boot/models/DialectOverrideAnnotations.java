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
import org.hibernate.models.Creator;
import org.hibernate.models.spi.MutableAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface DialectOverrideAnnotations {
	MutableAnnotationDescriptor<DialectOverride.Checks, OverriddenChecksAnnotation> DIALECT_OVERRIDE_CHECKS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Checks.class,
			OverriddenChecksAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.Check, OverriddenCheckAnnotation> DIALECT_OVERRIDE_CHECK = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Check.class,
			OverriddenCheckAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_CHECKS
	);
	MutableAnnotationDescriptor<DialectOverride.ColumnDefaults, OverriddenColumnDefaultsAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULTS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.ColumnDefaults.class,
			OverriddenColumnDefaultsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.ColumnDefault, OverriddenColumnDefaultAnnotation> DIALECT_OVERRIDE_COLUMN_DEFAULT = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.ColumnDefault.class,
			OverriddenColumnDefaultAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_COLUMN_DEFAULTS
	);
	MutableAnnotationDescriptor<DialectOverride.GeneratedColumns, OverriddenGeneratedColumnsAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMNS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.GeneratedColumns.class,
			OverriddenGeneratedColumnsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.GeneratedColumn, OverriddenGeneratedColumnAnnotation> DIALECT_OVERRIDE_GENERATED_COLUMN = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.GeneratedColumn.class,
			OverriddenGeneratedColumnAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_GENERATED_COLUMNS
	);
	MutableAnnotationDescriptor<DialectOverride.DiscriminatorFormulas, OverriddenDiscriminatorFormulasAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.DiscriminatorFormulas.class,
			OverriddenDiscriminatorFormulasAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.DiscriminatorFormula, OverriddenDiscriminatorFormulaAnnotation> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.DiscriminatorFormula.class,
			OverriddenDiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS
	);
	MutableAnnotationDescriptor<FetchProfileOverrides, FetchProfileOverridesAnnotation> FETCH_PROFILE_OVERRIDES = Creator.createCompleteAnnotationDescriptor(
			FetchProfileOverrides.class,
			FetchProfileOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<FetchProfileOverride, FetchProfileOverrideAnnotation> FETCH_PROFILE_OVERRIDE = Creator.createCompleteAnnotationDescriptor(
			FetchProfileOverride.class,
			FetchProfileOverrideAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			FETCH_PROFILE_OVERRIDES
	);
	MutableAnnotationDescriptor<DialectOverride.FilterOverrides, OverriddenFilterOverridesAnnotation> DIALECT_OVERRIDE_FILTER_OVERRIDES = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.FilterOverrides.class,
			OverriddenFilterOverridesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.Filters, OverriddenFiltersAnnotation> DIALECT_OVERRIDE_FILTERS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Filters.class,
			OverriddenFiltersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_OVERRIDES
	);
	MutableAnnotationDescriptor<DialectOverride.FilterDefOverrides, OverriddenFilterDefOverridesAnnotation> DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.FilterDefOverrides.class,
			OverriddenFilterDefOverridesAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.FilterDefs, OverriddenFilterDefsAnnotation> DIALECT_OVERRIDE_FILTER_DEFS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.FilterDefs.class,
			OverriddenFilterDefsAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES
	);
	MutableAnnotationDescriptor<DialectOverride.Formulas, OverriddenFormulasAnnotation> DIALECT_OVERRIDE_FORMULAS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Formulas.class,
			OverriddenFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.Formula, OverriddenFormulaAnnotation> DIALECT_OVERRIDE_FORMULA = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Formula.class,
			OverriddenFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_FORMULAS
	);
	MutableAnnotationDescriptor<DialectOverride.JoinFormulas, OverriddenJoinFormulasAnnotation> DIALECT_OVERRIDE_JOIN_FORMULAS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.JoinFormulas.class,
			OverriddenJoinFormulasAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.JoinFormula, OverriddenJoinFormulaAnnotation> DIALECT_OVERRIDE_JOIN_FORMULA = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.JoinFormula.class,
			OverriddenJoinFormulaAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_JOIN_FORMULAS
	);
	MutableAnnotationDescriptor<DialectOverride.SQLInserts, OverriddenSQLInsertsAnnotation> DIALECT_OVERRIDE_SQL_INSERTS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLInserts.class,
			OverriddenSQLInsertsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLInsert, OverriddenSQLInsertAnnotation> DIALECT_OVERRIDE_SQL_INSERT = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLInsert.class,
			OverriddenSQLInsertAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_INSERTS
	);
	MutableAnnotationDescriptor<DialectOverride.SQLUpdates, OverriddenSQLUpdatesAnnotation> DIALECT_OVERRIDE_SQL_UPDATES = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLUpdates.class,
			OverriddenSQLUpdatesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLUpdate, OverriddenSQLUpdateAnnotation> DIALECT_OVERRIDE_SQL_UPDATE = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLUpdate.class,
			OverriddenSQLUpdateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_UPDATES
	);
	MutableAnnotationDescriptor<DialectOverride.SQLDeletes, OverriddenSQLDeletesAnnotation> DIALECT_OVERRIDE_SQL_DELETES = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLDeletes.class,
			OverriddenSQLDeletesAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLDelete, OverriddenSQLDeleteAnnotation> DIALECT_OVERRIDE_SQL_DELETE = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLDelete.class,
			OverriddenSQLDeleteAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETES
	);
	MutableAnnotationDescriptor<DialectOverride.SQLDeleteAlls, OverriddenSQLDeleteAllsAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALLS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLDeleteAlls.class,
			OverriddenSQLDeleteAllsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLDeleteAll, OverriddenSQLDeleteAllAnnotation> DIALECT_OVERRIDE_SQL_DELETE_ALL = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLDeleteAll.class,
			OverriddenSQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_DELETE_ALLS
	);
	MutableAnnotationDescriptor<DialectOverride.SQLOrders, OverriddenSQLOrdersAnnotation> DIALECT_OVERRIDE_SQL_ORDERS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLOrders.class,
			OverriddenSQLOrdersAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLOrder, OverriddenSQLOrderAnnotation> DIALECT_OVERRIDE_SQL_ORDER = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLOrder.class,
			OverriddenSQLOrderAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false,
			DIALECT_OVERRIDE_SQL_ORDERS
	);
	MutableAnnotationDescriptor<DialectOverride.SQLRestrictions, OverriddenSQLRestrictionsAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTIONS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLRestrictions.class,
			OverriddenSQLRestrictionsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLRestriction, OverriddenSQLRestrictionAnnotation> DIALECT_OVERRIDE_SQL_RESTRICTION = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLRestriction.class,
			OverriddenSQLRestrictionAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_RESTRICTIONS
	);
	MutableAnnotationDescriptor<DialectOverride.SQLSelects, OverriddenSQLSelectsAnnotation> DIALECT_OVERRIDE_SQL_SELECTS = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLSelects.class,
			OverriddenSQLSelectsAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DialectOverride.SQLSelect, OverriddenSQLSelectAnnotation> DIALECT_OVERRIDE_SQL_SELECT = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.SQLSelect.class,
			OverriddenSQLSelectAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD, Kind.CLASS ),
			false,
			DIALECT_OVERRIDE_SQL_SELECTS
	);
	MutableAnnotationDescriptor<DialectOverride.Version, OverrideVersionAnnotation> DIALECT_OVERRIDE_VERSION = Creator.createCompleteAnnotationDescriptor(
			DialectOverride.Version.class,
			OverrideVersionAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( DialectOverrideAnnotations.class, consumer );
	}
}
