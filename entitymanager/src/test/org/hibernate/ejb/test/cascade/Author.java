//$Id: $
package org.hibernate.ejb.test.cascade;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.SequenceGenerator;
import javax.persistence.GenerationType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Author {
	@Id 	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ENTITY2_SEQ")
	@SequenceGenerator(name = "ENTITY2_SEQ")
 private Long id;

}
