package org.hibernate.processor.test.namedquery;

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
@SqlResultSetMapping(name = "bookNativeQueryResult")
@SqlResultSetMappings({@SqlResultSetMapping(name="result set mapping one"), @SqlResultSetMapping(name = "result_set-mapping-two")})
public class Main {
}
