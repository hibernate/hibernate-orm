package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Runs {@link AbstractImmutableLazyBasicWithAssociationCacheTest} against the annotation mapping.
 */
@DomainModel(
		annotatedClasses = {
				AbstractImmutableLazyBasicWithAssociationCacheTest.DataHolder.class,
				AbstractImmutableLazyBasicWithAssociationCacheTest.DataHolderNoLazyCache.class,
				AbstractImmutableLazyBasicWithAssociationCacheTest.Category.class
		}
)
public class ImmutableLazyBasicWithAssociationCacheAnnotationTest
		extends AbstractImmutableLazyBasicWithAssociationCacheTest {
}
