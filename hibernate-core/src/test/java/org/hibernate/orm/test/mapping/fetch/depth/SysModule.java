/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.fetch.depth;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_mod")
public class SysModule {
	@Id
	private Integer id;

	@Column(name = "name")
	private String name;

//    @OneToMany( cascade = CascadeType.PERSIST, fetch = FetchType.EAGER )
//    @JoinColumn( name = "target_mod_fk" )
	@ManyToMany( targetEntity = SysModule.class, cascade = { CascadeType.PERSIST }, fetch = FetchType.EAGER )
	@JoinTable(
			name = "sys_group_mod",
			joinColumns = @JoinColumn(name = "src_fk", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "target_fk", referencedColumnName = "id")
	)
	private Set<SysModule> targetModules;
}
