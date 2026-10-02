package org.hibernate.orm.test.jpa.cascade;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Author {
@Id @GeneratedValue
private Long id;

}
