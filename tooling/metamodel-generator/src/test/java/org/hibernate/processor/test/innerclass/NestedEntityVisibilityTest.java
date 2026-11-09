package org.hibernate.processor.test.innerclass;

import java.lang.reflect.ParameterizedType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQuery;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CompilationTest
@TestForIssue(jiraKey = "HHH-21095")
@WithClasses(value = NestedEntityVisibilityTest.class,
		sources = "org.hibernate.processor.test.innerclass.hidden.OtherModel")
class NestedEntityVisibilityTest {
	@Entity(name = "NestedBook")
	static class Book {
		@Id
		long id;
	}

	@MappedSuperclass
	@NamedQuery(name = "books", query = "from NestedBook")
	@NamedQuery(name = "hiddenBooks", query = "from HiddenBook")
	static class Queries {
	}

	@Test
	void infersOnlyAccessibleQueryResultTypes() {
		var books = getFieldFromMetamodelFor( Queries.class, "_books_" );
		var hiddenBooks = getFieldFromMetamodelFor( Queries.class, "_hiddenBooks_" );
		assertEquals( Book.class, ( (ParameterizedType) books.getGenericType() ).getActualTypeArguments()[0] );
		assertEquals( Object.class, ( (ParameterizedType) hiddenBooks.getGenericType() ).getActualTypeArguments()[0] );
	}
}
