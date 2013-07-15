//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;
import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Acces implements Serializable {
	@Id
	private BigInteger idpk;

	@ManyToOne
	private Droitacces idpkdracc;
}
