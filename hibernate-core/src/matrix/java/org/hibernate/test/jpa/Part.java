package org.hibernate.test.jpa;
import java.math.BigDecimal;

/**
 * @author Steve Ebersole
 */
public class Part {
	private Long id;
	private Item item;
	private String name;
	private String stockNumber;
	private BigDecimal unitPrice;

	public Part() {
	}

	public Part(Item item, String name, String stockNumber, BigDecimal unitPrice) {
		this.item = item;
		this.name = name;
		this.stockNumber = stockNumber;
		this.unitPrice = unitPrice;

		this.item.getParts().add( this );
	}

	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public Item getItem() {
		return item;
	}

	private void setItem(Item item) {
		this.item = item;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStockNumber() {
		return stockNumber;
	}

	public void setStockNumber(String stockNumber) {
		this.stockNumber = stockNumber;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}
}
