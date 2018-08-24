/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;


@Entity
public class MutableEntity{

    @Id
    @GeneratedValue
    private Long id;

    private String changeable;

    @OneToOne
    private ImmutableEntity trouble;

    public MutableEntity() {
    }

    public MutableEntity(ImmutableEntity trouble, String changeable) {
        this.trouble = trouble;
        this.changeable = changeable;
    }

    public Long getId() {
        return id;
    }

    public ImmutableEntity getTrouble() {
        return trouble;
    }

    public String getChangeable() {
        return changeable;
    }

    public void setChangeable(String changeable) {
        this.changeable = changeable;
    }
}