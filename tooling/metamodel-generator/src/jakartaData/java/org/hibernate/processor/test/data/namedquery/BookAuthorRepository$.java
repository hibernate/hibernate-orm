package org.hibernate.processor.test.data.namedquery;

import jakarta.persistence.NamedQuery;

@NamedQuery(name = "findByTitleLike",
			query = "from Book where title like :title")
public interface BookAuthorRepository$ {
}
