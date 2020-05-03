/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.uniqueconstraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * @author <a href="mailto:bernhardt.manuel@gmail.com">Manuel Bernhardt</a>
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "uniqueWithInherited", columnNames = {"room_id", "cost"} )})
public class House extends Building {
	@Column(nullable = false)
    public Long id;
	@NotNull
    public Integer cost;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }
}
