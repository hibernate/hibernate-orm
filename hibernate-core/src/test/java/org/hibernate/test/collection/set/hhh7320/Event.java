package org.hibernate.test.collection.set.hhh7320;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Entity
public class Event implements Serializable {

	private static final long serialVersionUID = -2376468057851018229L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "localized_event", joinColumns = { @JoinColumn(
			name = "localized_id",
			referencedColumnName = "id",
			nullable = false,
			insertable = false,
			updatable = false) })
	@MapKeyColumn(
			name = "locale",
			nullable = false,
			insertable = false,
			updatable = false,
			length = 2)
	private Map<Locale, LocalizedEvent> localized;
}
