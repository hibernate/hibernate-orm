@Entity
public class Product {
	...
	@Lob
	@Basic
	public String description;
	...
}