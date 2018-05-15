/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "USERS")
public class User {
	@Id
	@GeneratedValue
	@Column(name = "USER_ID")
	private long id;

	@OneToOne(targetEntity = UserSetting.class)
	@JoinColumn(name = "USER_SETTING_ID", foreignKey = @ForeignKey(name = "FK_TO_USER_SETTING"))
	private UserSetting userSetting;

	@OneToMany(targetEntity = Group.class)
	@JoinColumn(name = "USER_ID", foreignKey = @ForeignKey(name = "FK_USER_GROUP"))
	private List<Group> groups;
}
