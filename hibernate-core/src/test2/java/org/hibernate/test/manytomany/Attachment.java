/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Set;

/**
 * @author Chris Cranford
 */
@Entity
public class Attachment {
    @Id
    @GeneratedValue
    private Integer id;

    @ManyToMany
    private Set<Advertisement> advertisements;

    private String fileName;

    private String deleted = "false";

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Advertisement> getAdvertisements() {
        return advertisements;
    }

    public void setAdvertisements(Set<Advertisement> advertisements) {
        this.advertisements = advertisements;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
