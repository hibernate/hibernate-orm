package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = { "/org/hibernate/orm/test/mapping/collections/custom/parameterized/Mapping.orm.xml" }
)
public class ParameterizedUserCollectionTypeXmlVariantTest extends ParameterizedUserCollectionTypeTest {
}
