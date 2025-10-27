import { Injectable, OnModuleInit } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { User } from './users/user.entity';
import { Product } from './products/product.entity';
import { Order } from './orders/order.entity';
import { OrderItem } from './orders/orderItem.entity';

@Injectable()
export class SeederService implements OnModuleInit {
  constructor(
    @InjectRepository(User) private users: Repository<User>,
    @InjectRepository(Product) private products: Repository<Product>,
    @InjectRepository(Order) private orders: Repository<Order>,
    @InjectRepository(OrderItem) private items: Repository<OrderItem>,
  ) {}

  async onModuleInit() {
    const usersCount = await this.users.count();
    if (usersCount === 0) {
      const u = this.users.create({ name: 'Admin', email: 'admin@example.com', passwordHash: await bcrypt.hash('admin123', 10) });
      await this.users.save(u);
    }

    const productCount = await this.products.count();
    if (productCount === 0) {
      const samples = [
        { sku: 'INV-001', name: 'Inverter', category: 'cat1', price: 1200, stock: 82 },
        { sku: 'BAT-002', name: 'Battery', category: 'cat2', price: 300, stock: 5 },
        { sku: 'GEN-003', name: 'Generator', category: 'cat3', price: 2500, stock: 60 },
        { sku: 'CHR-004', name: 'Charger', category: 'cat3', price: 150, stock: 12 },
        { sku: 'PWR-005', name: 'Power', category: 'cat4', price: 99, stock: 2 },
      ];
      for (const s of samples) {
        const p = this.products.create(s as any);
        await this.products.save(p);
      }
    }

    const ordersCount = await this.orders.count();
    if (ordersCount === 0) {
      const admin = await this.users.findOne({ where: { email: 'admin@example.com' } });
      const prods = await this.products.find();
      if (admin && prods.length) {
        const o = this.orders.create({ customer: admin, salesChannel: 'online', destination: 'warehouse', status: 'completed' });
        o.items = [
          this.items.create({ product: prods[0], quantity: 2 }),
          this.items.create({ product: prods[1], quantity: 1 }),
        ];
        await this.orders.save(o);
      }
    }
  }
}
