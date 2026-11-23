package org.hibernate.orm.test.bulkid;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class DefaultMutationStrategyGeneratedIdentityTest extends AbstractMutationStrategyGeneratedIdentityTest {

}
