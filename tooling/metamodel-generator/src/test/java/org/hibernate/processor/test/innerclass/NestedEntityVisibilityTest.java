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
	@NamedQuery(name = "declaredBooks", query = "from NestedBook")
	static class Book {
		@Id
		long id;
	}

	@Entity
	@NamedQuery(name = "otherBooks", query = "from NestedBook")
	static class Magazine {
		@Id
		long id;
	}

	@MappedSuperclass
	@NamedQuery(name = "books", query = "from NestedBook")
	@NamedQuery(name = "hiddenBooks", query = "from HiddenBook")
	@NamedQuery(name = "magazines", query = "from Magazine")
	@NamedQuery(name = "typedBooks", query = "from NestedBook", resultClass = Book.class)
	@NamedQuery(name = "pairs", query = "select b.id, b.id from NestedBook b")
	@NamedQuery(name = "categories", query = "select a.category from Article a")
	static class Queries {
	}

	@Test
	void infersOnlyAccessibleQueryResultTypes() {
		assertResultType( Queries.class, "_books_", Object.class );
		assertResultType( Book.class, "_declaredBooks_", Book.class );
		assertResultType( Magazine.class, "_otherBooks_", Object.class );
		assertResultType( Queries.class, "_hiddenBooks_", Object.class );
		assertResultType( Queries.class, "_magazines_", Magazine.class );
		assertResultType( Queries.class, "_typedBooks_", Book.class );
		assertResultType( Queries.class, "_pairs_", Object[].class );
		assertResultType( Queries.class, "_categories_", Object.class );
	}

	private static void assertResultType(Class<?> owner, String fieldName, Class<?> expected) {
		var field = getFieldFromMetamodelFor( owner, fieldName );
		assertEquals( expected, ( (ParameterizedType) field.getGenericType() ).getActualTypeArguments()[0] );
	}
}
