package org.hibernate.test.querycache;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EntityWithStringCompositeKey {

	private StringCompositeKey pk;

	@EmbeddedId
	public StringCompositeKey getPk() {
		return pk;
	}

	public void setPk(StringCompositeKey pk) {
		this.pk = pk;
	}
}
