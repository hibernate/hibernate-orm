package org.hibernate.orm.test.annotations.generics;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Paper extends Item<PaperType, SomeGuy> {
}
