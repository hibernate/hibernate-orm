package org.hibernate.processor.test.spi;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@ExtensionMarker
public class SpiBook {
	@Id
	Long id;
	String title;
}
