package org.hibernate.orm.test.query.named;

import java.util.List;

import jakarta.data.repository.Param;
import jakarta.persistence.query.JakartaQuery;

public interface BookRepositoryQueries {
	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	List<Book> inheritedFindByTitle(@Param("title") String title);
}
