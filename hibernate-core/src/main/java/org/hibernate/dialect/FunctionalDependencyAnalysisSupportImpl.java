/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * @author Marco Belladelli
 */
public class FunctionalDependencyAnalysisSupportImpl implements FunctionalDependencyAnalysisSupport {
	private final boolean supportsAnalysis;
	private final boolean supportsTableGroups;
	private final boolean supportsConstants;

	/**
	 * No support for functional dependency analysis
	 */
	public static final FunctionalDependencyAnalysisSupportImpl NONE = new FunctionalDependencyAnalysisSupportImpl(
			false,
			false,
			false
	);

	/**
	 * Only supports the analysis for a single table reference, i.e. no support for joins / unions
	 */
	public static final FunctionalDependencyAnalysisSupportImpl TABLE_REFERENCE = new FunctionalDependencyAnalysisSupportImpl(
			true,
			false,
			false
	);

	/**
	 * Supports the analysis for single tables, a group of joined tables or a result set (e.g. union)
	 * as long as only table columns are selected, i.e. no constants (see {@link #TABLE_GROUP_AND_CONSTANTS})
	 */
	public static final FunctionalDependencyAnalysisSupportImpl TABLE_GROUP = new FunctionalDependencyAnalysisSupportImpl(
			true,
			true,
			false
	);

	/**
	 * Fully supports the analysis for joined / union table groups, including any constant value
	 * (e.g. the literal {@code clazz_} column used as table per class inheritance discriminator column)
	 */
	public static final FunctionalDependencyAnalysisSupportImpl TABLE_GROUP_AND_CONSTANTS = new FunctionalDependencyAnalysisSupportImpl(
			true,
			true,
			true
	);

	public FunctionalDependencyAnalysisSupportImpl(
			boolean supportsAnalysis,
			boolean supportsTableGroups,
			boolean supportsConstants) {
		this.supportsAnalysis = supportsAnalysis;
		this.supportsTableGroups = supportsTableGroups;
		this.supportsConstants = supportsConstants;
	}

	@Override
	public boolean supportsAnalysis() {
		return supportsAnalysis;
	}

	@Override
	public boolean supportsTableGroups() {
		return supportsTableGroups;
	}

	@Override
	public boolean supportsConstants() {
		return supportsConstants;
	}
}
