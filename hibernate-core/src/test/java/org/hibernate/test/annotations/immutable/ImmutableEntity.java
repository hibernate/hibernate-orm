/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Immutable
public class ImmutableEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String selector;

    public ImmutableEntity() {
    }

    public ImmutableEntity(String selector) {
        this.selector = selector;
    }

    public Long getId() {
        return id;
    }

    public String getSelector() {
        return selector;
    }

}