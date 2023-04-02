package org.hibernate.jpamodelgen.test.records;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.Assert.assertNotNull;

public class Java14RecordsTest extends CompilationTest {

    @Test
    @TestForIssue(jiraKey = "HHH-16261")
    @WithClasses({Address.class, Author.class})
    public void testEmbeddableRecordProperty() {
        assertMetamodelClassGeneratedFor(Address.class);
        assertMetamodelClassGeneratedFor(Author.class);
        assertNotNull("Author must contain 'address' field", getFieldFromMetamodelFor(Author.class, "address"));
    }
}
