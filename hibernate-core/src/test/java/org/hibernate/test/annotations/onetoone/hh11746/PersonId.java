/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hh11746;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Jonathan Ihm
 */
@Embeddable
public class PersonId implements Serializable {

    private String id;
    private String socialId;

    public PersonId() {
    }

    public PersonId(String id, String socialId) {
        this.id = id;
        this.socialId = socialId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSocialId() {
        return socialId;
    }

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonId)) return false;
        PersonId that = (PersonId) o;
        return Objects.equals(getId(), that.getId()) &&
                Objects.equals(getSocialId(), that.getSocialId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSocialId());
    }
}
