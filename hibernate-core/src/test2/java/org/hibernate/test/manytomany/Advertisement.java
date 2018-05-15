/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany;

import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import java.util.Set;

/**
 * @author Chris Cranford
 */
@Entity
public class Advertisement {
    @Id
    @GeneratedValue
    private Integer id;

    @Where(clause = "deleted <> 'true'")
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "advertisements")
    @OrderBy("id asc")
    private Set<Attachment> attachments;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = attachments;
    }
}
