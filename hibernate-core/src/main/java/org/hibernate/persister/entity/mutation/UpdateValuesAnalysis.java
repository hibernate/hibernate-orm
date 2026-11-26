package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

/**
 * Contains an aggregated analysis of the values for an update mutation
 * to determine behavior such as skipping tables which contained no changes,
 * etc.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.2")
public interface UpdateValuesAnalysis extends ValuesAnalysis {
	@Nullable
	Object[] getValues();

	/**
	 * Descriptor of the tables needing to be updated.
	 *
	 * @apiNote {@linkplain TableMapping#isInverse() Inverse tables} are not included in the result
	 */
	@Nonnull
	TableSet getTablesNeedingUpdate();

	/**
	 * Descriptor of the tables which had any non-null value bindings
	 */
	@Nonnull
	TableSet getTablesWithNonNullValues();

	/**
	 * Descriptor of the tables which had any non-null value bindings
	 */
	@Nonnull
	TableSet getTablesWithPreviousNonNullValues();

	@Nonnull
	TableSet getTablesNeedingDynamicUpdate();

	/**
	 * Descriptors for the analysis of each attribute
	 */
	@Nonnull
	List<AttributeAnalysis> getAttributeAnalyses();
}
