package org.hibernate.envers.test.integration.inheritance.joined.relation;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "APP_RIGHTSSUBJCT")
@Inheritance(strategy = InheritanceType.JOINED)
@Audited
public class RightsSubject {
    @Id
    @GeneratedValue
    private Long id;
    
    @Version
    private Long version;

    @Column(name = "APP_GROUP")
    private String group;

    @ManyToMany(mappedBy="members")
    private Set<Role> roles = new HashSet<Role>();

    public RightsSubject() {
    }

    public RightsSubject(Long id, String group) {
        this.id = id;
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RightsSubject)) return false;

        RightsSubject that = (RightsSubject) o;

        if (group != null ? !group.equals(that.group) : that.group != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (group != null ? group.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RightsSubject(id = " + id + ", version = " + version + ", group = " + group + ")";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
