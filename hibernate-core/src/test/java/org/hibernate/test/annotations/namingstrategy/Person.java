// $Id$
package org.hibernate.test.annotations.namingstrategy;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
		uniqueConstraints = {@UniqueConstraint(columnNames = "bio"),
				@UniqueConstraint(name = "uk_nickname", columnNames = "nickname")},
		indexes = {@Index(columnList = "favoriteColor", unique = true, name = "uk_color"),
				@Index(columnList = "favoriteBand", name = "idx_band"),
				@Index(columnList = "favoriteSong")})
public class Person {

	@Id
	private long id;
	
	@Column(unique = true)
	private String name;
	
	private String nickname;
	
	private String bio;
	
	private String favoriteColor;
	
	private String favoriteBand;
	
	private String favoriteSong;

	@OneToMany(mappedBy = "person")
	private Set<Address> addresses = new HashSet<Address>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getFavoriteColor() {
		return favoriteColor;
	}

	public void setFavoriteColor(String favoriteColor) {
		this.favoriteColor = favoriteColor;
	}

	public String getFavoriteBand() {
		return favoriteBand;
	}

	public void setFavoriteBand(String favoriteBand) {
		this.favoriteBand = favoriteBand;
	}
}
