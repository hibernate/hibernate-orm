/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class EnumEntity {
    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_option")
    @Audited(modifiedColumnName = "client_option_mod")
    private EnumOption option;

    EnumEntity() {

    }

    public EnumEntity(Integer id, EnumOption option) {
        this.id = id;
        this.option = option;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EnumOption getOption() {
        return option;
    }

    public void setOption(EnumOption option) {
        this.option = option;
    }
}
