export class CreateOrderDto {
  customerId: number;
  salesChannel?: string;
  destination?: string;
  items: { productId: number; quantity: number }[];
}

