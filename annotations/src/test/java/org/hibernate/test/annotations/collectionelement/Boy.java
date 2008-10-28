//$Id$
package org.hibernate.test.annotations.collectionelement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AttributeOverrides({
		@AttributeOverride( name="characters.element", column = @Column(name="fld_character") ),
		@AttributeOverride( name="scorePerNickName.element", column = @Column(name="fld_score") ),
		@AttributeOverride( name="favoriteToys.element.brand.surname", column = @Column(name = "fld_surname"))}
)
public class Boy {
	private Integer id;
	private String firstName;
	private String lastName;
	private Set<String> nickNames = new HashSet<String>();
	private Map<String, Integer> scorePerNickName = new HashMap<String, Integer>();
	private int[] favoriteNumbers;
	private Set<Toy> favoriteToys = new HashSet<Toy>();
	private Set<Character> characters = new HashSet<Character>();
	private Set<CountryAttitude> countryAttitudes = new HashSet<CountryAttitude>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@CollectionOfElements
	public Set<String> getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set<String> nickName) {
		this.nickNames = nickName;
	}

	@CollectionOfElements
	@JoinTable(name = "ScorePerNickName", joinColumns = @JoinColumn(name = "BoyId"))
	@Column(name = "score", nullable = false)
	public Map<String, Integer> getScorePerNickName() {
		return scorePerNickName;
	}

	public void setScorePerNickName(Map<String, Integer> scorePerNickName) {
		this.scorePerNickName = scorePerNickName;
	}

	@CollectionOfElements
	@JoinTable(
			name = "BoyFavoriteNumbers",
			joinColumns = @JoinColumn(name = "BoyId")
	)
	@Column(name = "favoriteNumber", nullable = false)
	@IndexColumn(name = "nbr_index")
	public int[] getFavoriteNumbers() {
		return favoriteNumbers;
	}

	public void setFavoriteNumbers(int[] favoriteNumbers) {
		this.favoriteNumbers = favoriteNumbers;
	}

	@CollectionOfElements
	@AttributeOverride(name = "element.serial", column = @Column(name = "serial_nbr"))
	public Set<Toy> getFavoriteToys() {
		return favoriteToys;
	}

	public void setFavoriteToys(Set<Toy> favoriteToys) {
		this.favoriteToys = favoriteToys;
	}

	@CollectionOfElements
	@Enumerated(EnumType.STRING)
	public Set<Character> getCharacters() {
		return characters;
	}

	public void setCharacters(Set<Character> characters) {
		this.characters = characters;
	}

	@CollectionOfElements(fetch = FetchType.EAGER)
	//@Where(clause = "b_likes=false")
	public Set<CountryAttitude> getCountryAttitudes() {
		return countryAttitudes;
	}

	public void setCountryAttitudes(Set<CountryAttitude> countryAttitudes) {
		this.countryAttitudes = countryAttitudes;
	}
}

