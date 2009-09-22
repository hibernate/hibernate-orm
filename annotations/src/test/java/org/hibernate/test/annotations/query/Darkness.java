package org.hibernate.test.annotations.query;

import javax.persistence.MappedSuperclass;

@org.hibernate.annotations.NamedQuery(
		name = "night.olderThan", 
		query = "select n from Night n where n.date <= :date"
)

@MappedSuperclass
public class Darkness {

}
