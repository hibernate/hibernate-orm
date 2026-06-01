/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;

@NamedQueries(@NamedQuery(name = "#bookByIsbn", query = "from Book where isbn = :isbn"))
@NamedQuery(name = "#bookByTitle", query = "from Book where title = :title")
@FetchProfile(name = "dummy-fetch")
@FetchProfiles({@FetchProfile(name = "fetch.one"), @FetchProfile(name = "fetch#two")})
@NamedNativeQuery(name = "bookNativeQuery", query = "select * from Book")
@NamedNativeQueries(@NamedNativeQuery(name = "(sysdate)", query = "select sysdate from dual"))
@SqlResultSetMapping(name = "bookNativeQueryResult", entities = @EntityResult(entityClass = Book.class))
@SqlResultSetMappings({
		@SqlResultSetMapping(
				name = "result set mapping one",
				columns = @ColumnResult(name = "title", type = String.class)
		),
		@SqlResultSetMapping(
				name = "result_set-mapping-two",
				classes = @ConstructorResult(
						targetClass = TitleAndIsbn.class,
						columns = {
								@ColumnResult(name = "title"),
								@ColumnResult(name = "isbn")
						}
				)
		),
		@SqlResultSetMapping(
				name = "compound mapping",
				entities = @EntityResult(entityClass = Book.class),
				columns = @ColumnResult(name = "title", type = String.class)
		)
})
public class Main {
}
