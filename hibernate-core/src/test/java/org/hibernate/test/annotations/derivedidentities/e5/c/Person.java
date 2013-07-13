package org.hibernate.test.annotations.derivedidentities.e5.c;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@Id @GeneratedValue
	Integer id;
}
