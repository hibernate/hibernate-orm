//$Id$
package org.hibernate.jpa.test.pack.defaultpar_1_0;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ApplicationServer1 {
	private Integer id;
	private String name;
	private Version1 version;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Embedded
	public Version1 getVersion() {
		return version;
	}

	public void setVersion(Version1 version) {
		this.version = version;
	}
}