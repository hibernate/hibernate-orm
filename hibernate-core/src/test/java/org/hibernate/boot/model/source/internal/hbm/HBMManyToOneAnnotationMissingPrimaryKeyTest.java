package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * https://hibernate.atlassian.net/browse/HHH-11502
 *
 * @author Russ Tennant (russ@venturetech.net)
 */
public class HBMManyToOneAnnotationMissingPrimaryKeyTest extends BaseNonConfigCoreFunctionalTestCase
{
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                AnnotationEntity.class
        };
    }

    @Override
    protected String[] getMappings() {
        return new String[]{
                "HBMEntity.hbm.xml"
        };
    }

    @Override
    protected String getBaseForMappings() {
        return "/org/hibernate/boot/model/source/internal/hbm/";
    }

    /**
     * Test to trigger the NullPointerException in the ModelBinder.
     * @throws Exception on error.
     */
    @Test
    public void hhh11502() throws Exception {
        Assert.assertTrue(true);
    }
}
