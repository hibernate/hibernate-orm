package org.hibernate.jpamodelgen.test.collectiontype;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Created by helloztt on 2017-08-19.
 */
public class CollectionTest extends CompilationTest {

    @Test
    @WithClasses({Goods.class, Product.class})
    public void testPrimitiveArray() {
        assertMetamodelClassGeneratedFor(Product.class);
        assertMetamodelClassGeneratedFor(Goods.class);
    }
}
