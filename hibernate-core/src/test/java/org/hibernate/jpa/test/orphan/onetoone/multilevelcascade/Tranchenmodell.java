/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
