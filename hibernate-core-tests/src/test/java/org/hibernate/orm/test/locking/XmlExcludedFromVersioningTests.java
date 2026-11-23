package org.hibernate.orm.test.locking;

import org.hibernate.testing.orm.junit.DomainModel;

/// Variant of {@link ExcludedFromVersioningTestsBase} mapping the very same {@link Phone}/{@link Counter}
/// classes via `metadata-complete="true"` orm.xml, so their `@ExcludedFromVersioning` annotations are
/// ignored and the equivalent `<exclude-from-versioning/>` orm.xml element is exercised instead.
@DomainModel(xmlMappings = "org/hibernate/orm/test/locking/ExcludedFromVersioning.xml")
public class XmlExcludedFromVersioningTests extends ExcludedFromVersioningTestsBase {
}
