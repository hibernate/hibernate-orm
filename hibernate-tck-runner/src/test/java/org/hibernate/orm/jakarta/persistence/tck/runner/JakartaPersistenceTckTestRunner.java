/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.persistence.tck.runner;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Jakarta Persistence TCK tests Runner")
// Defines a "root" package, subpackages are included.
// Use Include/Exclude ClassNamePatterns annotations to limit the executed tests:
@SelectPackages({"ee.jakarta.tck.persistence"})
@IncludeClassNamePatterns({".*Client.*"})
// If you want to run this from an IDE (like IDEA... )
//  make sure to include `-Pdb=pgsql_ci` at the very end of the `Run` input of your run configuration
//  Failing that ^ just comment out the test.onlyIf part in the Gradle config of this module...
public class JakartaPersistenceTckTestRunner {

}
