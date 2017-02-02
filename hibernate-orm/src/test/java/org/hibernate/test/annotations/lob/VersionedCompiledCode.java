/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Compiled code representation with a version
 *
 * @author Gail Badner
 */
@Entity
public class VersionedCompiledCode extends AbstractCompiledCode{
	private Integer id;
	private Integer version;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Version
	@Column(name = "ver")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer i) {
		version = i;
	}
}
