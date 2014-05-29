package org.hibernate.metamodel.archive.internal;

import java.net.URL;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;

public class ArchiveHelperTest extends BaseUnitTestCase {
    @Test
    public void testGetJarURLFromURLEntry() throws Exception {
        Assert.assertEquals(
                "Regular jar path",
                "file:/path/to/lib.jar",
                ArchiveHelper.getJarURLFromURLEntry(new URL("jar:file:/path/to/lib.jar!/path/to/entry"), "path/to/entry").toExternalForm());
        Assert.assertEquals(
                "Uber jar path",
                "jar:file:/path/to/uber.jar!/lib.jar",
                ArchiveHelper.getJarURLFromURLEntry(new URL("jar:file:/path/to/uber.jar!/lib.jar!/path/to/entry"), "path/to/entry").toExternalForm());
    }
}
