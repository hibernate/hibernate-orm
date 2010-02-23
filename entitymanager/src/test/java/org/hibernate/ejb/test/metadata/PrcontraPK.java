// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.ejb.test.metadata;

import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class PrcontraPK implements Serializable {
	private static final long serialVersionUID = 7441961985141369232L;
	@Column(name = "NUMERO", nullable = false)
	private BigInteger numero;
	@Column(name = "RENOV", nullable = false)
	private BigInteger renov;
	@Column(name = "TIPO", nullable = false)
	private BigInteger tipo;

	public PrcontraPK() {
	}

	public PrcontraPK(BigInteger numero, BigInteger renov, BigInteger tipo) {
		this.numero = numero;
		this.renov = renov;
		this.tipo = tipo;
	}

	public BigInteger getNumero() {
		return numero;
	}

	public void setNumero(BigInteger numero) {
		this.numero = numero;
	}

	public BigInteger getRenov() {
		return renov;
	}

	public void setRenov(BigInteger renov) {
		this.renov = renov;
	}

	public BigInteger getTipo() {
		return tipo;
	}

	public void setTipo(BigInteger tipo) {
		this.tipo = tipo;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += ( numero != null ? numero.hashCode() : 0 );
		hash += ( renov != null ? renov.hashCode() : 0 );
		hash += ( tipo != null ? tipo.hashCode() : 0 );
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if ( !( object instanceof PrcontraPK ) ) {
			return false;
		}
		PrcontraPK other = ( PrcontraPK ) object;
		if ( this.numero != other.numero && ( this.numero == null || !this.numero.equals( other.numero ) ) ) {
			return false;
		}
		if ( this.renov != other.renov && ( this.renov == null || !this.renov.equals( other.renov ) ) ) {
			return false;
		}
		if ( this.tipo != other.tipo && ( this.tipo == null || !this.tipo.equals( other.tipo ) ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "org.kyrian.entity.muvale.PrcontraPK[numero=" + numero + ", renov=" + renov + ", tipo=" + tipo + "]";
	}
}


