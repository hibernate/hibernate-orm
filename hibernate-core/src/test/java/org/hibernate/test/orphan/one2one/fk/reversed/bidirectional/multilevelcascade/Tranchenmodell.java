/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one.fk.reversed.bidirectional.multilevelcascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class Tranchenmodell {


    private Long id;

    private List<Tranche> tranchen = new ArrayList<Tranche>();


    private Preisregelung preisregelung;


    @Id
    public Long getId() {
        return id;
    }

	public void setId(Long id) {
		this.id = id;
	}

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tranchenmodell", fetch = FetchType.LAZY, orphanRemoval = true)
    public List<Tranche> getTranchen() {
        return tranchen;
    }

    public void setTranchen(List<Tranche> tranchen) {
        this.tranchen = tranchen;
    }

    @OneToOne(mappedBy="tranchenmodell", optional = true, fetch = FetchType.LAZY)
    public Preisregelung getPreisregelung() {
        return preisregelung;
    }

    public void setPreisregelung(Preisregelung preisregelung) {
        this.preisregelung = preisregelung;
    }


}
