//$Id$
package org.hibernate.test.quote;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`Role`")
public class Role implements Serializable {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

}
