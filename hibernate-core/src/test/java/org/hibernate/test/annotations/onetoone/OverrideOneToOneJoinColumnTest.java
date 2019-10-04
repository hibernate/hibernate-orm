package org.hibernate.test.annotations.onetoone;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Aresnii Skvortsov
 */
@TestForIssue(jiraKey = "HHH-4384")
public class OverrideOneToOneJoinColumnTest extends BaseUnitTestCase {
	@Test
	public void allowIfJoinColumnIsAbsent() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			Metadata metadata = new MetadataSources(ssr)
				.addInputStream(getClass().getResourceAsStream("override-absent-join-column.orm.xml"))
				.buildMetadata();

			Table childTable = metadata.getDatabase().getDefaultNamespace().locateTable(Identifier.toIdentifier("Son"));
			ForeignKey fatherFk = (ForeignKey) childTable.getForeignKeyIterator().next();
			assertEquals("Overridden join column name should be applied", fatherFk.getColumn(0).getName(), "id_father");
		} finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}

	@Test
	public void disallowOnSideWithMappedBy() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			new MetadataSources(ssr)
				.addInputStream(getClass().getResourceAsStream("join-column-on-mapped-by.orm.xml"))
				.buildMetadata();
			fail("Should disallow @JoinColumn override on side with mappedBy");
		} catch (AnnotationException ex) {
			assertTrue("Should disallow exactly because of @JoinColumn override on side with mappedBy",
					   ex
						   .getMessage()
						   .startsWith("Illegal attempt to define a @JoinColumn with a mappedBy association:")
			);
		} finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}
}
