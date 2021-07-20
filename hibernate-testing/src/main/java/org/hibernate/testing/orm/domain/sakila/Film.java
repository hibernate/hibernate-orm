/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity
@Table( name = "film" )
public class Film {
	private Integer id;
	private String title;
	private String description;
	private Integer releaseYear;
	private Language language;
	private Language originalLanguage;
	private Integer rentalDuration = 3;
	private BigDecimal rentalRate = new BigDecimal("4.99");
	private Integer length;
	private BigDecimal replacementCost = new BigDecimal( "19.99" );
	private Rating rating = Rating.G;
	private Set<FilmSpecialFeature> specialFeatures;
	private Set<Actor> actors;
	private Set<Category> categories;
	private LocalDateTime lastUpdate;

	public Film() {
	}

	public Film(
			Integer id,
			String title,
			String description,
			Integer releaseYear,
			Language language,
			Language originalLanguage,
			Integer rentalDuration,
			BigDecimal rentalRate,
			Integer length,
			BigDecimal replacementCost,
			Rating rating,
			Set<FilmSpecialFeature> specialFeatures,
			Set<Actor> actors, Set<Category> categories) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.releaseYear = releaseYear;
		this.language = language;
		this.originalLanguage = originalLanguage;
		this.rentalDuration = rentalDuration;
		this.rentalRate = rentalRate;
		this.length = length;
		this.replacementCost = replacementCost;
		this.rating = rating;
		this.specialFeatures = specialFeatures;
		this.actors = actors;
		this.categories = categories;
	}

	@Id
	@Column( name = "film_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column( name = "title", nullable = false )
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Column( name = "description" )
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column( name = "release_year" )
	public Integer getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(Integer releaseYear) {
		this.releaseYear = releaseYear;
	}

	@ManyToOne
	@JoinColumn( name = "language_id" )
	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	@ManyToOne
	@JoinColumn( name = "original_language_id" )
	public Language getOriginalLanguage() {
		return originalLanguage;
	}

	public void setOriginalLanguage(Language originalLanguage) {
		this.originalLanguage = originalLanguage;
	}

	@Column( name = "rental_duration", nullable = false )
	public Integer getRentalDuration() {
		return rentalDuration;
	}

	public void setRentalDuration(Integer rentalDuration) {
		this.rentalDuration = rentalDuration;
	}

	@Column( name = "rental_rate", nullable = false, precision = 4, scale = 2 )
	public BigDecimal getRentalRate() {
		return rentalRate;
	}

	public void setRentalRate(BigDecimal rentalRate) {
		this.rentalRate = rentalRate;
	}

	@Column( name = "length" )
	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	@Column( name = "replacement_cost", nullable = false, precision = 5, scale = 2 )
	public BigDecimal getReplacementCost() {
		return replacementCost;
	}

	public void setReplacementCost(BigDecimal replacementCost) {
		this.replacementCost = replacementCost;
	}

	@Column( name = "rating", nullable = false )
	@Convert( converter = RatingConverter.class )
	public Rating getRating() {
		return rating;
	}

	public void setRating(Rating rating) {
		this.rating = rating;
	}

	@ElementCollection
	@Column( name = "special_feature" )
	@Convert( converter = FilmSpecialFeatureConverter.class )
	@CollectionTable(
			name = "film_special_features",
			joinColumns = @JoinColumn( name = "film_id" )
	)
	public Set<FilmSpecialFeature> getSpecialFeatures() {
		return specialFeatures;
	}

	public void setSpecialFeatures(Set<FilmSpecialFeature> specialFeatures) {
		this.specialFeatures = specialFeatures;
	}

	@ManyToMany
	@JoinTable(
			name = "film_actor",
			joinColumns = @JoinColumn( name = "film_id" ),
			inverseJoinColumns = @JoinColumn( name = "actor_id" )
	)
	public Set<Actor> getActors() {
		return actors;
	}

	public void setActors(Set<Actor> actors) {
		this.actors = actors;
	}

	@ManyToMany
	@JoinTable(
			name = "film_category",
			joinColumns = @JoinColumn( name = "film_id" ),
			inverseJoinColumns = @JoinColumn( name = "category_id" )
	)
	public Set<Category> getCategories() {
		return categories;
	}

	public void setCategories(Set<Category> categories) {
		this.categories = categories;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	static class RatingConverter implements AttributeConverter<Rating, String> {

		@Override
		public String convertToDatabaseColumn(Rating rating) {
			return rating.toString();
		}

		@Override
		public Rating convertToEntityAttribute(String text) {
			return Rating.fromText( text );
		}
	}

	static class FilmSpecialFeatureConverter implements AttributeConverter<FilmSpecialFeature, String> {
		@Override
		public String convertToDatabaseColumn(FilmSpecialFeature filmSpecialFeature) {
			return filmSpecialFeature.toString();
		}

		@Override
		public FilmSpecialFeature convertToEntityAttribute(String text) {
			return FilmSpecialFeature.fromText( text );
		}
	}
}
