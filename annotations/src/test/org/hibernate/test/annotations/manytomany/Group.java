//$Id$
package org.hibernate.test.annotations.manytomany;

import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.WhereJoinTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_group")
@FilterDef(name="Groupfilter")
public class Group {
	private Integer id;
	private Collection<Permission> permissions;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToMany(cascade = CascadeType.PERSIST)
	@JoinTable(name = "GROUPS_PERMISSIONS",
			uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "permission"}),
			joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "permission", referencedColumnName = "permission")
	)
	@OrderBy("expirationDate")
	@Where(clause = "1=1")
	@WhereJoinTable(clause = "2=2")
	@Filter(name="Groupfilter", condition = "3=3")
	@FilterJoinTable(name="Groupfilter", condition = "4=4")
	public Collection<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Collection<Permission> permissions) {
		this.permissions = permissions;
	}
}
