/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AttributeOverrides({
		@AttributeOverride( name="characters.element", column = @Column(name="fld_character") ),
		@AttributeOverride( name="scorePerNickName.element", column = @Column(name="fld_score") ),
		@AttributeOverride( name="favoriteToys.element.brand.surname", column = @Column(name = "fld_surname"))}
)
@Table(name="tbl_Boys")
public class Boy {
	private Integer id;
	private String firstName;
	private String lastName;
	private Set<String> nickNames = new HashSet<>();
	private Set<String> hatedNames = new HashSet<>();
	private Set<String> preferredNames = new HashSet<>();
	private Map<String, Integer> scorePerNickName = new HashMap<>();
	private Map<String, Integer> scorePerPreferredName = new HashMap<>();
	private int[] favoriteNumbers;
	private Set<Toy> favoriteToys = new HashSet<>();
	private Set<CharacterTrait> characters = new HashSet<>();
	private Map<String, FavoriteFood> foods = new HashMap<>();
	private Set<CountryAttitude> countryAttitudes = new HashSet<>();

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

	@ElementCollection
	public Set<String> getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set<String> nickName) {
		this.nickNames = nickName;
	}

	@ElementCollection //default column names
	public Set<String> getHatedNames() {
		return hatedNames;
	}

	public void setHatedNames(Set<String> hatedNames) {
		this.hatedNames = hatedNames;
	}

	@ElementCollection //default column names
	@Column
	public Set<String> getPreferredNames() {
		return preferredNames;
	}

	public void setPreferredNames(Set<String> preferredNames) {
		this.preferredNames = preferredNames;
	}

	@ElementCollection
	@MapKeyColumn(nullable=false)
	public Map<String, Integer> getScorePerPreferredName() {
		return scorePerPreferredName;
	}

	public void setScorePerPreferredName(Map<String, Integer> scorePerPreferredName) {
		this.scorePerPreferredName = scorePerPreferredName;
	}

	@ElementCollection
	@CollectionTable(name = "ScorePerNickName", joinColumns = @JoinColumn(name = "BoyId"))
	@Column(name = "score", nullable = false)
	@MapKeyColumn(nullable=false)
	public Map<String, Integer> getScorePerNickName() {
		return scorePerNickName;
	}

	public void setScorePerNickName(Map<String, Integer> scorePerNickName) {
		this.scorePerNickName = scorePerNickName;
	}

	@ElementCollection
	@CollectionTable(
			name = "BoyFavoriteNumbers",
			joinColumns = @JoinColumn(name = "BoyId")
	)
	@Column(name = "favoriteNumber", nullable = false)
	@OrderColumn(name = "nbr_index")
	public int[] getFavoriteNumbers() {
		return favoriteNumbers;
	}

	public void setFavoriteNumbers(int[] favoriteNumbers) {
		this.favoriteNumbers = favoriteNumbers;
	}
	@ElementCollection
	@AttributeOverride(name = "element.serial", column = @Column(name = "serial_nbr"))
	public Set<Toy> getFavoriteToys() {
		return favoriteToys;
	}

	public void setFavoriteToys(Set<Toy> favoriteToys) {
		this.favoriteToys = favoriteToys;
	}

	@ElementCollection
	@Enumerated(EnumType.STRING)
	@Column(name = "`characters`")
	public Set<CharacterTrait> getCharacters() {
		return characters;
	}

	public void setCharacters(Set<CharacterTrait> characters) {
		this.characters = characters;
	}

	@ElementCollection
	@Enumerated(EnumType.STRING)
	@MapKeyColumn(nullable=false)
	public Map<String, FavoriteFood> getFavoriteFood() {
		return foods;
	}

	public void setFavoriteFood(Map<String, FavoriteFood>foods) {
		this.foods = foods;
	}

	@ElementCollection(fetch = FetchType.EAGER)
	//@Where(clause = "b_likes=false")
	public Set<CountryAttitude> getCountryAttitudes() {
		return countryAttitudes;
	}

	public void setCountryAttitudes(Set<CountryAttitude> countryAttitudes) {
		this.countryAttitudes = countryAttitudes;
	}
}
