/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Jakarta Data TCK tests Runner")
@SelectPackages({"org.hibernate.orm.jakarta.data.tck.runner"})
@IncludeClassNamePatterns({".*Standalone.*"})
@IncludeTags("standalone & persistence")
// Tests to challenge:
@ExcludeClassNamePatterns({
		// Sort nullable tests - nulls ordering differences.
		// This test relies on a new feature that is not yet published in any M* Data releases,
		// hence not implemented yet.
		".*StandaloneSortNullableTests",
})
public class JakartaDataTckTestRunner {

}
