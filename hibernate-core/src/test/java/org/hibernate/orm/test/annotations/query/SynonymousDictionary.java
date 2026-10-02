package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("Syn")
public class SynonymousDictionary extends Dictionary {
}
