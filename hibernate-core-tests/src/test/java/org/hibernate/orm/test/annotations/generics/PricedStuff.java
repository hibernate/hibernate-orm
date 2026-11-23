package org.hibernate.orm.test.annotations.generics;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class PricedStuff extends Stuff<Price> {
}
