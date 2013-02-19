package org.hibernate.jpa.test.metamodel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity fot HHH-7985
 *
 * @author Nikolay Shestakov
 */
@Entity
@Table( name = "SONG_TABLE" )
public class Song {

	public enum Type {
		PRAISE
	}

	@Id
	int id;
	long totalDownloads;
	Float weight;
	double price;
	Type type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTotalDownloads() {
		return totalDownloads;
	}

	public void setTotalDownloads(long totalDownloads) {
		this.totalDownloads = totalDownloads;
	}

	public Float getWeight() {
		return weight;
	}

	public void setWeight(Float weight) {
		this.weight = weight;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
