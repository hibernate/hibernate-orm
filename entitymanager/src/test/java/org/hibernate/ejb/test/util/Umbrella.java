package org.hibernate.ejb.test.util;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EmbeddedId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Umbrella {
	private PK id;

	private int size;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public static class PK implements Serializable {
		private String model;
		private String brand;

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getBrand() {
			return brand;
		}

		public void setBrand(String brand) {
			this.brand = brand;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PK pk = ( PK ) o;

			if ( brand != null ? !brand.equals( pk.brand ) : pk.brand != null ) {
				return false;
			}
			if ( model != null ? !model.equals( pk.model ) : pk.model != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = model != null ? model.hashCode() : 0;
			result = 31 * result + ( brand != null ? brand.hashCode() : 0 );
			return result;
		}
	}
}
