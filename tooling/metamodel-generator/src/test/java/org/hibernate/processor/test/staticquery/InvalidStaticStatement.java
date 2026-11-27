package org.hibernate.processor.test.staticquery;

import jakarta.persistence.query.NativeStatement;

import java.util.List;

public interface InvalidStaticStatement {
	@NativeStatement("delete from Book where obsolete = true")
	List<Book> deleteObsolete();
}
