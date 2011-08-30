//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;
import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.Id;


@Entity
public class Service {
    @Id
    private BigInteger idpk;
}
