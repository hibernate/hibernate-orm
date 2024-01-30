/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * Contains an aggregated analysis of the values for an update mutation
 * to determine behavior such as skipping tables which contained no changes,
 * etc.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface UpdateValuesAnalysis extends ValuesAnalysis {
	Object[] getValues();

	/**
	 * Descriptor of the tables needing to be updated.
	 *
	 * @apiNote {@linkplain TableMapping#isInverse() Inverse tables} are not included in the result
	 */
	TableSet getTablesNeedingUpdate();

	/**
	 * Descriptor of the tables which had any non-null value bindings
	 */
	TableSet getTablesWithNonNullValues();

	/**
	 * Descriptor of the tables which had any non-null value bindings
	 */
	TableSet getTablesWithPreviousNonNullValues();

	TableSet getTablesNeedingDynamicUpdate();

	/**
	 * Descriptors for the analysis of each attribute
	 */
	List<AttributeAnalysis> getAttributeAnalyses();
}
