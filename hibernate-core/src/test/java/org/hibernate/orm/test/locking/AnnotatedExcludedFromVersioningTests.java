package org.hibernate.orm.test.locking;

import org.hibernate.testing.orm.junit.DomainModel;

/// Variant of {@link ExcludedFromVersioningTestsBase} where {@link Phone}/{@link Counter} are mapped via
/// the `@jakarta.persistence.ExcludedFromVersioning` annotation already present on their fields.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		ExcludedFromVersioningTestsBase.Phone.class,
		ExcludedFromVersioningTestsBase.Counter.class
})
public class AnnotatedExcludedFromVersioningTests extends ExcludedFromVersioningTestsBase {
}
