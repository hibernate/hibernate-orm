package org.hibernate.test.annotations.id.generationmappings;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity(name = DedicatedSequenceEntity2.ENTITY_NAME)
@Table(name = "DEDICATED_SEQ_TBL2")
public class DedicatedSequenceEntity2 implements Serializable {
	public static final String ENTITY_NAME = "DEDICATED2";

	private Long id;

	@Id
	@GeneratedValue(generator = "SequencePerEntityGenerator")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
