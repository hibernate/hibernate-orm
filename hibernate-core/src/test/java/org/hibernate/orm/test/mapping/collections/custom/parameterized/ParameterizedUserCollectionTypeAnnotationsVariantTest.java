package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {Entity.class}
)
public class ParameterizedUserCollectionTypeAnnotationsVariantTest extends ParameterizedUserCollectionTypeTest {
}
