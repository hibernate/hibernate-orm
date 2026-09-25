package org.hibernate.orm.test.annotations.inheritance.singletable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("2")
public class Rock extends Music {
}
