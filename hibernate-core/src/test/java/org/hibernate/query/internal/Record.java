package org.hibernate.query.internal;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Four {@link OffsetDateTime} attributes, all carrying the very same {@code @Convert(converter = ...)}.
 * <p>
 * The point of interest is that they are declared separately: Hibernate instantiates one
 * {@code AttributeConverterBean} per attribute, so the four attributes end up with four distinct converter
 * <em>instances</em> of the same converter class.
 */
@Entity
@Table(name = "A_RECORD")
class Record {

	@Id
	private Long id;

	@Convert(converter = UtcOffsetConverter.class)
	private OffsetDateTime validFrom;

	@Convert(converter = UtcOffsetConverter.class)
	private OffsetDateTime validTo;

	@Convert(converter = UtcOffsetConverter.class)
	private OffsetDateTime recordedFrom;

	@Convert(converter = UtcOffsetConverter.class)
	private OffsetDateTime recordedTo;

	protected Record() {
		// for JPA
	}

	public Record(Long id, OffsetDateTime validFrom, OffsetDateTime validTo,
				OffsetDateTime recordedFrom, OffsetDateTime recordedTo) {
		this.id = id;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.recordedFrom = recordedFrom;
		this.recordedTo = recordedTo;
	}

	public Long getId() {
		return id;
	}

	public OffsetDateTime getValidFrom() {
		return validFrom;
	}

}
