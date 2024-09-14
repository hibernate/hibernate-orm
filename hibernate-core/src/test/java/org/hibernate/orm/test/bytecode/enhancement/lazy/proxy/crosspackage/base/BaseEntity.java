/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Embedded
    protected EmbeddableType embeddedField;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public EmbeddableType getEmbeddedField() {
        return embeddedField;
    }

    public void setEmbeddedField(final EmbeddableType embeddedField) {
        this.embeddedField = embeddedField;
    }
}
