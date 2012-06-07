package org.hibernate.test.hql;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ComponentContainer {

	private Long id;
	private ComponentContainer.Address address;

	public ComponentContainer() {
	}

	public ComponentContainer(Address address) {
		this.address = address;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ComponentContainer.Address getAddress() {
		return address;
	}

	public void setAddress(ComponentContainer.Address address) {
		this.address = address;
	}

	public static class Address {
		private String street;
		private String city;
		private String state;
		private ComponentContainer.Address.Zip zip;

		public Address() {
		}

		public Address(String street, String city, String state, ComponentContainer.Address.Zip zip) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public ComponentContainer.Address.Zip getZip() {
			return zip;
		}

		public void setZip(ComponentContainer.Address.Zip zip) {
			this.zip = zip;
		}

		public static class Zip {
			private int code;
			private int plus4;

			public Zip() {
			}

			public Zip(int code, int plus4) {
				this.code = code;
				this.plus4 = plus4;
			}

			public int getCode() {
				return code;
			}

			public void setCode(int code) {
				this.code = code;
			}

			public int getPlus4() {
				return plus4;
			}

			public void setPlus4(int plus4) {
				this.plus4 = plus4;
			}
		}
	}

}
