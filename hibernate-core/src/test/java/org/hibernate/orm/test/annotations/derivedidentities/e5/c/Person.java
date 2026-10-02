package org.hibernate.orm.test.annotations.derivedidentities.e5.c;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@Id @GeneratedValue
	Integer id;
}
