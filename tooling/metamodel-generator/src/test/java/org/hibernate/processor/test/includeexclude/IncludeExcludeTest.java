package org.hibernate.processor.test.includeexclude;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

public class IncludeExcludeTest extends CompilationTest {
	@Test
	@WithClasses({ Foo.class, Bar.class, Baz.class })
	@WithProcessorOption(key = HibernateProcessor.INCLUDE, value = "org.hibernate.processor.test.includeexclude.*")
	@WithProcessorOption(key = HibernateProcessor.EXCLUDE, value = "org.hibernate.processor.test.includeexclude.F*")
	public void testQueryMethod() {
		assertNoMetamodelClassGeneratedFor( Foo.class );
		assertMetamodelClassGeneratedFor( Bar.class );
		assertNoMetamodelClassGeneratedFor( Baz.class );
	}
}
