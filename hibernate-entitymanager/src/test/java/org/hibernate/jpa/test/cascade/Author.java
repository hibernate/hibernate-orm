//$Id$
package org.hibernate.jpa.test.cascade;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Author {
 @Id @GeneratedValue
 private Long id;

}
