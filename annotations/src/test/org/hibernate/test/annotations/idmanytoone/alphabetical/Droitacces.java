//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import java.math.BigInteger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Droitacces {
	@Id
	private BigInteger idpk;

	@ManyToOne
	private Benefserv idpkbenef;
}
