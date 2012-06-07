//$Id$
package org.hibernate.test.annotations.tuplizer;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public interface Country {
	@Column(name = "CountryName")
	public String getName();
	public void setName(String name);
}
