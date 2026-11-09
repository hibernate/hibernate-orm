package org.hibernate.processor.test.innerclass.query;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQuery;

@MappedSuperclass
@NamedQuery(name = "inners", query = "from Inner")
public class InnerQueries {
}
