import { Controller, Get } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Order } from '../orders/order.entity';
import { Product } from '../products/product.entity';

@Controller('dashboard')
export class DashboardController {
  constructor(
    @InjectRepository(Order) private readonly ordersRepo: Repository<Order>,
    @InjectRepository(Product) private readonly productsRepo: Repository<Product>,
  ) {}

  @Get()
  async metrics() {
    const orders = await this.ordersRepo.find({ relations: ['items', 'items.product'] });
    const revenue = orders.reduce((sum, o) => sum + o.items.reduce((s, i) => s + Number(i.product?.price || 0) * i.quantity, 0), 0);

    const salesReturn = 30000; // placeholder for design symmetry
    const purchase = 30000; // placeholder
    const income = 30000; // placeholder

    const topSelling: Record<string, number> = {};
    for (const o of orders) {
      for (const i of o.items) {
        if (!i.product) continue;
        topSelling[i.product.name] = (topSelling[i.product.name] || 0) + i.quantity;
      }
    }
    const topSellingArray = Object.entries(topSelling)
      .map(([name, qty]) => ({ name, qty }))
      .sort((a, b) => b.qty - a.qty)
      .slice(0, 5);

    const stockAlert = (await this.productsRepo.find()).filter((p) => p.stock <= 5);

    return {
      cards: { revenue, salesReturn, purchase, income },
      topSelling: topSellingArray,
      stockAlert,
    };
  }
}

