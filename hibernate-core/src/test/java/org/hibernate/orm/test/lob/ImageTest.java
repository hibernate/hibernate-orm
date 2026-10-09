package org.hibernate.orm.test.lob;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#IMAGE}.
 *
 * @author Gail Badner
 */
@DomainModel(xmlMappings = "mappings/lob/ImageMappings.orm.xml")
public class ImageTest extends LongByteArrayTest {
}
