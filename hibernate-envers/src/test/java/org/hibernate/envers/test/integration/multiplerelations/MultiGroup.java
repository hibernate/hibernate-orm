/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;

/**
 * @author Naros (crancran at gmail dot com)
 */
@Entity
@Audited
public class MultiGroup {
	
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;

    @ManyToMany
    @OrderColumn
    private List<GroupMember> members = new ArrayList<GroupMember>();

    public void addMember(GroupMember item) {
    	members.add(item);        
    }

    @Override
    public String toString() {
    	return "MultiGroup [id=" + id + ", members.size=" + members.size() + "]";
    }
    
}
