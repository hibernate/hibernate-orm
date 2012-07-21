//$Id$
package org.hibernate.jpa.test.callbacks;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class CountryNameCheckerListener {
	private List<String> countries = new ArrayList<String>();

	{
		countries.add( "France" );
		countries.add( "Netherland" );
	}

	@PrePersist
	@PreUpdate
	public void testCountryName(Object object) {
		if ( object instanceof Translation ) {
			Translation tr = (Translation) object;
			if ( ! countries.contains( tr.getInto() ) ) {
				throw new IllegalArgumentException( "Not a country name: " + tr.getInto() );
			}
		}
	}
}
