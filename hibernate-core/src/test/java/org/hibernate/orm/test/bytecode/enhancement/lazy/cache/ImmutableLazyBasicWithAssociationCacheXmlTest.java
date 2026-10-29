package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Runs {@link AbstractImmutableLazyBasicWithAssociationCacheTest} against an equivalent
 * {@code metadata-complete} XML mapping, so annotations on the model are ignored.
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/bytecode/enhancement/lazy/cache/ImmutableLazyBasicWithAssociationCache.orm.xml"
)
public class ImmutableLazyBasicWithAssociationCacheXmlTest
		extends AbstractImmutableLazyBasicWithAssociationCacheTest {
}
