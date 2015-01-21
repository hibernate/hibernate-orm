/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.orphan.onetoone.multilevelcascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;

@Entity
public class Tranchenmodell {

    @Id
	@GeneratedValue
	private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tranchenmodell", fetch = FetchType.LAZY, orphanRemoval = true)
	@OrderColumn
    private List<Tranche> tranchen = new ArrayList<Tranche>();

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    private Preisregelung preisregelung;

	@OneToOne(mappedBy = "tranchenmodell", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private X x;

    public Long getId() {
        return id;
    }

	public void setId(Long id) {
		this.id = id;
	}

    public List<Tranche> getTranchen() {
        return tranchen;
    }

    public Preisregelung getPreisregelung() {
        return preisregelung;
    }

    public void setPreisregelung(Preisregelung preisregelung) {
        this.preisregelung = preisregelung;
    }

	public X getX() {
		return x;
	}

	public void setX(X x) {
		this.x = x;
	}
}
