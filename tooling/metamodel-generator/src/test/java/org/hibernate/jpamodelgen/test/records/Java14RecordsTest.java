package org.hibernate.jpamodelgen.test.records;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

public class Java14RecordsTest extends CompilationTest {

    @Test
    @TestForIssue(jiraKey = "HHH-16261")
    @WithClasses({Address.class, Author.class})
    public void testEmbeddableRecordProperty() {
        assertMetamodelClassGeneratedFor(Address.class);
        assertMetamodelClassGeneratedFor(Author.class);
    }
}
