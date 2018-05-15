/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.orphan.onetoone.multilevelcascade;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class Preisregelung {
    @Id
	@GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "preisregelung", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Tranchenmodell tranchenmodell;


    public Long getId() {
        return id;
    }

	public void setId(Long id) {
		this.id = id;
	}

    public Tranchenmodell getTranchenmodell() {
        return tranchenmodell;
    }

    public void setTranchenmodell(Tranchenmodell tranchenmodell) {
        this.tranchenmodell = tranchenmodell;
    }
}
