package org.hibernate.orm.test.query.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "OtherEntity")
@Table(name = "OtherEntity")
public class OtherEntity {
	@Id
	Integer id;
}
