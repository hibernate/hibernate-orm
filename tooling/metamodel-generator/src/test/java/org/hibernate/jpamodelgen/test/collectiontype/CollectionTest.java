package org.hibernate.jpamodelgen.test.collectiontype;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import java.util.List;

import static org.hibernate.jpamodelgen.test.util.TestUtil.*;

/**
 * Created by helloztt on 2017-08-19.
 */
public class CollectionTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "HHH-12338")
	@WithClasses({Goods.class, Product.class})
	public void testPrimitiveArray() throws ClassNotFoundException {
		assertMetamodelClassGeneratedFor(Product.class);
		assertMetamodelClassGeneratedFor(Goods.class);
		assertListAttributeTypeInMetaModelFor(
				Goods.class,
				"productList",
				Product.class,
				"ListAttribute generic type should be Product"
		);
		assertAttributeTypeInMetaModelFor(
				Goods.class,
				"tags",
				List.class,
				"Wrong meta model type"
		);

	}
}
