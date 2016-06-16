/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

/**
 * @author Naros (crancran at gmail dot com)
 */
@Entity
@Audited
public class UniqueGroup {
	
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;

    @OneToMany
    @JoinColumn(name = "uniqueGroup_id")
    @AuditMappedBy(mappedBy = "uniqueGroup")
    private Set<GroupMember> members = new HashSet<GroupMember>();

    public Long getId() {
        return id;
    }

    public void addMember(GroupMember item) {    	
        members.add(item);        
    }

    @Override
    public String toString() {
    	return "UniqueGroup [id=" + id + ", members.size=" + members.size() + "]";
    }
    
}
