package org.hibernate.processor.test.nativequery;

import java.util.List;

import jakarta.data.repository.Param;
import jakarta.persistence.query.NativeQuery;
import org.hibernate.Session;

public interface DeferredNativeQueries {

	Session session();

	// Resolving the generated metamodel constant requires another processing round.
	// Will trigger the redo branch in HibernateProcessor
	@NativeQuery("select title from RedoBook where title = :title")
	List<String> findByTitle(@Param(RedoBook_.TITLE) String title);
}
