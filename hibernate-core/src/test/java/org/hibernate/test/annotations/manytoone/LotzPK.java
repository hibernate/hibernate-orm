//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class LotzPK implements Serializable {
	@Column( name = "id", nullable = false )
	private Integer id;

	@Column( name = "loc_code", nullable = false, unique = true )
	private String locCode;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLocCode() {
		return locCode;
	}

	public void setLocCode(String locCode) {
		this.locCode = locCode;
	}
}
