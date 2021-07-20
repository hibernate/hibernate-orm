/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idclass;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class NestedIdClassTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Asset.class, AssetAssetTypeAttribute.class, AssetTypeAttribute.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-14340")
	public void testIdClass() {
		inTransaction( entityManager -> {
			Asset asset = new Asset();
			asset.setId(1L);
			asset.setTenantId(2L);
			AssetTypeAttribute assetTypeAttribute = new AssetTypeAttribute();
			assetTypeAttribute.setId(3L);
			assetTypeAttribute.setName("TestAttribute");

			AssetAssetTypeAttribute assetAssetTypeAttribute = new AssetAssetTypeAttribute();

			assetAssetTypeAttribute.setAssetTypeAttributeId(assetTypeAttribute.getId());
			assetAssetTypeAttribute.setAsset(asset);
			asset.setAssetAssetTypeAttributes(new HashSet<>());
			asset.getAssetAssetTypeAttributes().add(assetAssetTypeAttribute);

			entityManager.persist(asset);

			for (AssetAssetTypeAttribute assetAssetTypeAttribute1 : asset.getAssetAssetTypeAttributes()) {
				entityManager.persist(assetAssetTypeAttribute1);
			}
		} );
	}

	@Entity(name = "Asset")
	@Table(name = "asset")
	@IdClass(AssetId.class)
	public static class Asset {
		@Id
		private Long id;

		@Id
		@Column(name = "tenant_id")
		private Long tenantId;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<AssetAssetTypeAttribute> assetAssetTypeAttributes;

		public void setTenantId(Long tenantId) {
			this.tenantId = tenantId;
		}

		public Long getTenantId() {
			return tenantId;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Set<AssetAssetTypeAttribute> getAssetAssetTypeAttributes() {
			return assetAssetTypeAttributes;
		}

		public void setAssetAssetTypeAttributes(Set<AssetAssetTypeAttribute> assetAssetTypeAttributes) {
			this.assetAssetTypeAttributes = assetAssetTypeAttributes;
		}
	}

	@Entity(name = "AssetAssetTypeAttribute")
	@Table(name = "asset_asset_type_attribute")
	@IdClass(AssetAttributeId.class)
	public static class AssetAssetTypeAttribute {

		@Id
		@Column(name = "tenant_id", insertable = false, updatable = false)
		private Long tenantId;

		@Id
		@Column(name = "asset_id", insertable = false, updatable = false)
		private Long assetId;

		@Id
		@Column(name = "asset_type_attribute_id")
		private Long assetTypeAttributeId;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "asset_id", referencedColumnName = "id"),
				@JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
		})
		private Asset asset;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
		@JoinColumn(name = "asset_type_attribute_id", referencedColumnName = "id", insertable = false, updatable = false)
		private AssetTypeAttribute assetTypeAttribute;

		private String value;

		public Long getTenantId() {
			return tenantId;
		}

		public void setTenantId(Long tenantId) {
			this.tenantId = tenantId;
		}

		public Long getAssetId() {
			return assetId;
		}

		public void setAssetId(Long assetId) {
			this.assetId = assetId;
		}

		public Long getAssetTypeAttributeId() {
			return assetTypeAttributeId;
		}

		public void setAssetTypeAttributeId(Long assetTypeAttributeId) {
			this.assetTypeAttributeId = assetTypeAttributeId;
		}

		public Asset getAsset() {
			return asset;
		}

		public void setAsset(Asset asset) {
			this.asset = asset;
		}

		public AssetTypeAttribute getAssetTypeAttribute() {
			return assetTypeAttribute;
		}

		public void setAssetTypeAttribute(AssetTypeAttribute assetTypeAttribute) {
			this.assetTypeAttribute = assetTypeAttribute;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
	@Entity(name = "AssetTypeAttribute")
	@Table(name = "asset_type_attribute")
	public static class AssetTypeAttribute {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "assetTypeAttribute", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<AssetAssetTypeAttribute> assetAssetTypeAttributes;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class AssetAttributeId implements Serializable {
		private Long assetId;
		private Long assetTypeAttributeId;
		private Long tenantId;

		public AssetAttributeId() {}

		public AssetAttributeId(Long assetId, Long assetTypeAttributeId, Long tenantId) {
			this.assetId = assetId;
			this.assetTypeAttributeId = assetTypeAttributeId;
			this.tenantId = tenantId;
		}

		public Long getAssetId() {
			return assetId;
		}

		public void setAssetId(Long assetId) {
			this.assetId = assetId;
		}

		public Long getAssetTypeAttributeId() {
			return assetTypeAttributeId;
		}

		public void setAssetTypeAttributeId(Long assetTypeAttributeId) {
			this.assetTypeAttributeId = assetTypeAttributeId;
		}

		public Long getTenantId() {
			return tenantId;
		}

		public void setTenantId(Long tenantId) {
			this.tenantId = tenantId;
		}
	}

	public static class AssetId implements Serializable {
		private Long id;
		private Long tenantId;

		public AssetId() {}

		public AssetId(Long id, Long tenantId) {
			this.id = id;
			this.tenantId = tenantId;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getTenantId() {
			return tenantId;
		}

		public void setTenantId(Long tenantId) {
			this.tenantId = tenantId;
		}
	}
}

