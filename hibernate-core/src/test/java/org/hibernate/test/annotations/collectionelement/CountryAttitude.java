//$Id$
package org.hibernate.test.annotations.collectionelement;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import org.hibernate.test.annotations.Country;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class CountryAttitude {
	private Boy boy;
	private Country country;
	private boolean likes;

	// TODO: This currently does not work
//	@ManyToOne(optional = false)
//	public Boy getBoy() {
//		return boy;
//	}

	public void setBoy(Boy boy) {
		this.boy = boy;
	}

	@ManyToOne(optional = false)
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Column(name = "b_likes")
	public boolean isLikes() {
		return likes;
	}

	public void setLikes(boolean likes) {
		this.likes = likes;
	}

	@Override
	public int hashCode() {
		return country.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof CountryAttitude ) ) {
			return false;
		}
		return country.equals( ( (CountryAttitude) obj ).country );
	}
}
