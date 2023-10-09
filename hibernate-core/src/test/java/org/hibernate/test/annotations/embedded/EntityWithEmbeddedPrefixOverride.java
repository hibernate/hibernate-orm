/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.EmbeddedPrefixOverride;

/**
 * @author Kevin Dargel
 */
@Entity
@Table(name="TableEmbeddedPrefixOverride")
public class EntityWithEmbeddedPrefixOverride {
	@Id
	@GeneratedValue
	private Integer id;
	
	@Embedded
	@EmbeddedPrefixOverride(value = "overridden")
	private EmbeddableExample embed;

	@Embedded
	private EmbeddableExample embedNotOverridden;

	@Embedded
	private NestedEmbeddableExample nestedEmbedNotOverridden;

	@Embedded
	@EmbeddedPrefixOverride(value = "overriddenEmbedded")
	@AttributeOverride(name = "embed.someString", column = @Column(name = "nestedOverriddenColumn"))
	private NestedEmbeddableExample nestedEmbed;

	@Embeddable
	static class NestedEmbeddableExample {
		private Integer someInteger;

		@Embedded
		private EmbeddableExample embedNotOverridden;

		@Embedded
		@EmbeddedPrefixOverride(value = "overriddenEmbedNested")
		private EmbeddableExample embed;

		public Integer getSomeInteger() {
			return someInteger;
		}

		public void setSomeInteger(Integer someInteger) {
			this.someInteger = someInteger;
		}

		public EmbeddableExample getEmbedNotOverridden() {
			return embedNotOverridden;
		}

		public void setEmbedNotOverridden(EmbeddableExample embedNotOverridden) {
			this.embedNotOverridden = embedNotOverridden;
		}

		public EmbeddableExample getEmbed() {
			return embed;
		}

		public void setEmbed(EmbeddableExample embed) {
			this.embed = embed;
		}
	}

	@Embeddable
	static class EmbeddableExample {
		private String someString;

		public String getSomeString() {
			return someString;
		}

		public void setSomeString(String someString) {
			this.someString = someString;
		}
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public EmbeddableExample getEmbed() {
		return embed;
	}

	public void setEmbed(EmbeddableExample embed) {
		this.embed = embed;
	}

	public EmbeddableExample getEmbedNotOverridden() {
		return embedNotOverridden;
	}

	public void setEmbedNotOverridden(EmbeddableExample embedNotOverridden) {
		this.embedNotOverridden = embedNotOverridden;
	}

	public NestedEmbeddableExample getNestedEmbedNotOverridden() {
		return nestedEmbedNotOverridden;
	}

	public void setNestedEmbedNotOverridden(NestedEmbeddableExample nestedEmbedNotOverridden) {
		this.nestedEmbedNotOverridden = nestedEmbedNotOverridden;
	}

	public NestedEmbeddableExample getNestedEmbed() {
		return nestedEmbed;
	}

	public void setNestedEmbed(NestedEmbeddableExample nestedEmbed) {
		this.nestedEmbed = nestedEmbed;
	}
}
