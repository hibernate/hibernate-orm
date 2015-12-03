Order order = new Order();
LineItem lineItem = new LineItem();
order.getLineItems().add( lineItem );

// This blows up (NPE) in normal Java usage
lineItem.getOrder.getname();