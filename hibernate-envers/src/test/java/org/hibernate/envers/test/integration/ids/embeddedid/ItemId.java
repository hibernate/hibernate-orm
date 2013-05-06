package org.hibernate.envers.test.integration.ids.embeddedid;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Embeddable
public class ItemId implements Serializable {
	@Column(name = "model")
	private String model;

	@Column(name = "version")
	private Integer version;

	@ManyToOne
	@JoinColumn(name = "producer", nullable = false) // NOT NULL for Sybase
	private Producer producer;

	public ItemId() {
	}

	public ItemId(String model, Integer version, Producer producer) {
		this.model = model;
		this.version = version;
		this.producer = producer;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ItemId) ) {
			return false;
		}

		ItemId itemId = (ItemId) o;

		if ( getModel() != null ? !getModel().equals( itemId.getModel() ) : itemId.getModel() != null ) {
			return false;
		}
		if ( getProducer() != null ? !getProducer().equals( itemId.getProducer() ) : itemId.getProducer() != null ) {
			return false;
		}
		if ( getVersion() != null ? !getVersion().equals( itemId.getVersion() ) : itemId.getVersion() != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = model != null ? model.hashCode() : 0;
		result = 31 * result + (version != null ? version.hashCode() : 0);
		result = 31 * result + (producer != null ? producer.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ItemId(model = " + model + ", version = " + version + ", producer = " + producer + ")";
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Producer getProducer() {
		return producer;
	}

	public void setProducer(Producer producer) {
		this.producer = producer;
	}
}