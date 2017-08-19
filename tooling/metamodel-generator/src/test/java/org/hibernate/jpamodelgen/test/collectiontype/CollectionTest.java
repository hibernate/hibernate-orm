package org.hibernate.jpamodelgen.test.collectiontype;

import org.hibernate.jpamodelgen.test.arraytype.Image;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.mapping.Collection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Created by helloztt on 2017-08-19.
 */
public class CollectionTest extends CompilationTest {

    @Test
    @WithClasses(Goods.class)
    public void testPrimitiveArray() {
        assertMetamodelClassGeneratedFor( Goods.class );
    }
}
