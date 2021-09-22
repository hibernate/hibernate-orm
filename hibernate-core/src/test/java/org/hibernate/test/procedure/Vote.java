/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Vote")
@Table(name = "vote")
public class Vote {

    @Id
    private Long id;

    @Column(name = "vote_choice")
    @Type(type = "yes_no")
    private boolean voteChoice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isVoteChoice() {
        return voteChoice;
    }

    public void setVoteChoice(boolean voteChoice) {
        this.voteChoice = voteChoice;
    }
}
