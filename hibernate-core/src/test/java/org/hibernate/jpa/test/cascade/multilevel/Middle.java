/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade.multilevel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "MIDDLE")
public class Middle {
    @Id
    private Long id;
    @ManyToOne
    private Top top;

    @OneToOne(cascade = { CascadeType.ALL })
    @JoinColumn(name = "BOTTOM_ID")
    private Bottom bottom;

    private Middle() {

    }

    public Middle(Long i) {
        this.id = i;
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    Top getTop() {
        return top;
    }

    void setTop(Top top) {
        this.top = top;
    }

    Bottom getBottom() {
        return bottom;
    }

    void setBottom(Bottom bottom) {
        this.bottom = bottom;
        bottom.setMiddle(this);
    }
}
