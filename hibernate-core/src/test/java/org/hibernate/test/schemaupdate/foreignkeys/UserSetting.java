/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "USER_SETTING")
public class UserSetting {
	@Id
	@GeneratedValue
	@Column(name = "USER_SETTING_ID")
	public long id;

	@OneToOne
	@JoinColumn(name = "USER_ID", foreignKey = @ForeignKey(name = "FK_TO_USER"))
	private User user;
}
